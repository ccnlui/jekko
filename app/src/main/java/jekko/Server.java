package jekko;

import java.util.concurrent.Callable;

import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import jekko.echonode.AeronEchoNode;
import jekko.echonode.EchoNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "server", usageHelpAutoWidth = true,
    description = "Echo back every message to client")
public class Server implements Callable<Void>
{
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "help message")
    boolean help;

    @Option(names = "--embedded-media-driver",
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

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    @Override
    public Void call() throws Exception
    {
        final MediaDriver mediaDriver = launchEmbeddedMediaDriverIfConfigured();
        final Aeron aeron = connectAeron(mediaDriver);

        final String inChannel = aeronIpcOrUdpChannel(subEndpoint);
        final int inStream = 4297;
        final Subscription sub = aeron.addSubscription(inChannel, inStream);

        LOG.info("server: in: {}:{}", inChannel, inStream);
        // LOG.info("server: out: {}:{}", outChannel, outStream);

        new AeronEchoNode(mediaDriver, aeron).run();

        closeIfNotNull(sub);
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

    private Aeron connectAeron(MediaDriver mediaDriver)
    {
        Aeron.Context aeronCtx = new Aeron.Context().idleStrategy(new NoOpIdleStrategy());
        if (mediaDriver != null)
        {
            aeronCtx = aeronCtx.aeronDirectoryName(mediaDriver.aeronDirectoryName());
        }
        else if (aeronDir != null)
        {
            aeronCtx = aeronCtx.aeronDirectoryName(aeronDir);
        }
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
