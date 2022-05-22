package jekko;

import java.util.concurrent.Callable;

import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import jekko.transceiver.AeronTransceiver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "client", usageHelpAutoWidth = true,
    description = "Send messages to echo server, measure RTT latencies in microseconds")
public class Client implements Callable<Void>
{
    @Option(names = {"-h", "--help"}, usageHelp = true,
        description = "help message")
    boolean help;

    @Option(names = "--embedded-media-driver", defaultValue = "false",
        description = "launch with embedded media driver (default ${DEFAULT-VALUE})")
    boolean embeddedMediaDriver;

    @Option(names = "--aeron-dir", description = "override directory name for embedded aeron media driver")
    String aeronDir;

    @Option(names = "--pub-endpoint", defaultValue = "",
        description = "aeron udp transport endpoint to which messages are published in address:port format (default: \"${DEFAULT-VALUE}\")")
    String pubEndpoint;

    @Option(names = "--sub-endpoint", defaultValue = "",
        description = "aeron udp transport endpoint from which messages are subscribed in address:port format (default: \"${DEFAULT-VALUE}\")")
    String subEndpoint;

    private static final Logger LOG = LoggerFactory.getLogger(Client.class);

    @Override
    public Void call() throws Exception
    {
        final String outChannel = aeronIpcOrUdpChannel(pubEndpoint);
        final int outStream = 4297;

        final MediaDriver mediaDriver = launchEmbeddedMediaDriverIfConfigured();
        String defaultAeronDirName = mediaDriver == null ? null : mediaDriver.aeronDirectoryName();
        final Aeron aeron = connectAeron(defaultAeronDirName);

        // construct publication and subscription
        final Publication pub = aeron.addPublication(outChannel, outStream);

        // LOG.info("client: in: {}:{}", inChannel, inStream);
        LOG.info("client: out: {}:{}", outChannel, outStream);

        AeronTransceiver transceiver = new AeronTransceiver(mediaDriver, aeron, embeddedMediaDriver);
        new LoadTestRig(transceiver).run();

        closeIfNotNull(pub);
        closeIfNotNull(aeron);
        closeIfNotNull(mediaDriver);
        return null;
    }

    private MediaDriver launchEmbeddedMediaDriverIfConfigured()
    {
        if (embeddedMediaDriver)
        {
            MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
                .dirDeleteOnStart(true)
                .threadingMode(ThreadingMode.DEDICATED)
                .conductorIdleStrategy(new BusySpinIdleStrategy())
                .senderIdleStrategy(new NoOpIdleStrategy())
                .receiverIdleStrategy(new NoOpIdleStrategy())
                .dirDeleteOnShutdown(true);
            if (aeronDir != null)
                mediaDriverCtx = mediaDriverCtx.aeronDirectoryName(aeronDir);
            MediaDriver md = MediaDriver.launchEmbedded(mediaDriverCtx);

            LOG.info(mediaDriverCtx.toString());
            return md;
        }
        return null;
    }

    private Aeron connectAeron(String defaultAeronDirName)
    {
        Aeron.Context aeronCtx = new Aeron.Context()
            .idleStrategy(new NoOpIdleStrategy());
        if (defaultAeronDirName != null)
            aeronCtx = aeronCtx.aeronDirectoryName(defaultAeronDirName);
        else if (aeronDir != null)
            aeronCtx = aeronCtx.aeronDirectoryName(aeronDir);
        LOG.info(aeronCtx.toString());

        final Aeron aeron = Aeron.connect(aeronCtx);
        return aeron;
    }

    private String aeronIpcOrUdpChannel(String endpoint)
    {
        if (endpoint == null || endpoint.isEmpty())
            return "aeron:ipc";
        else
            return "aeron:udp?endpoint=" + endpoint + "|mtu=1408";
    }

    private void closeIfNotNull(final AutoCloseable closeable) throws Exception
    {
        if (closeable != null)
            closeable.close();
    }
}
