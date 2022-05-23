package jekko;

import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;

public final class Util
{
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    private Util()
    {
    }

    public static MediaDriver launchEmbeddedMediaDriverIfConfigured()
    {
        if (Config.embeddedMediaDriver)
        {
            MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
                .dirDeleteOnStart(true)
                .threadingMode(ThreadingMode.DEDICATED)
                .conductorIdleStrategy(new BusySpinIdleStrategy())
                .senderIdleStrategy(new NoOpIdleStrategy())
                .receiverIdleStrategy(new NoOpIdleStrategy())
                .dirDeleteOnShutdown(true);
            if (Config.aeronDir != null)
            {
                mediaDriverCtx = mediaDriverCtx.aeronDirectoryName(Config.aeronDir);
            }
            LOG.info(mediaDriverCtx.toString());

            MediaDriver md = MediaDriver.launchEmbedded(mediaDriverCtx);
            return md;
        }
        return null;
    }

    public static Aeron connectAeron()
    {
        Aeron.Context aeronCtx = new Aeron.Context().idleStrategy(new NoOpIdleStrategy());
        if (Config.aeronDir != null)
        {
            aeronCtx = aeronCtx.aeronDirectoryName(Config.aeronDir);
        }
        LOG.info(aeronCtx.toString());

        return Aeron.connect(aeronCtx);
    }

    public static String aeronIpcOrUdpChannel(String endpoint)
    {
        if (endpoint == null || endpoint.isEmpty())
            return "aeron:ipc";
        else
            return "aeron:udp?endpoint=" + endpoint + "|mtu=1408";
    }

    public static void closeIfNotNull(final AutoCloseable closeable) throws Exception
    {
        if (closeable != null)
            closeable.close();
    }
}
