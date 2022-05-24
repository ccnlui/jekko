package jekko;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.function.BooleanSupplier;

import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.NanoClock;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.exceptions.AeronException;

final class Util
{
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);
    
    private Util()
    {
    }

    static final long NANOS_PER_SECOND = SECONDS.toNanos(1);
    static final long RECEIVE_DEADLINE_NS = SECONDS.toNanos(30);

    static MediaDriver launchEmbeddedMediaDriverIfConfigured()
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

    static Aeron connectAeron()
    {
        Aeron.Context aeronCtx = new Aeron.Context().idleStrategy(new NoOpIdleStrategy());
        if (Config.aeronDir != null)
        {
            aeronCtx = aeronCtx.aeronDirectoryName(Config.aeronDir);
        }
        LOG.info(aeronCtx.toString());

        return Aeron.connect(aeronCtx);
    }

    static String aeronIpcOrUdpChannel(String endpoint)
    {
        if (endpoint == null || endpoint.isEmpty())
            return "aeron:ipc";
        else
            return "aeron:udp?endpoint=" + endpoint + "|mtu=1408";
    }

    static void closeIfNotNull(final AutoCloseable closeable) throws Exception
    {
        if (closeable != null)
            closeable.close();
    }

    static void checkPublicationResult(final long result)
    {
        if (result == Publication.CLOSED ||
            result == Publication.NOT_CONNECTED ||
            result == Publication.MAX_POSITION_EXCEEDED)
        {
            throw new AeronException("Publication error: " + Publication.errorString(result));
        }
    }

    static boolean retryPublicationResult(final long result)
    {
        if (result == Publication.ADMIN_ACTION ||
            result == Publication.BACK_PRESSURED)
        {
            return true;
        }
        else if (result == Publication.CLOSED || 
            result == Publication.MAX_POSITION_EXCEEDED ||
            result == Publication.NOT_CONNECTED)
        {
            throw new AeronException("Publication error: " + Publication.errorString(result));
        }
        return false;
    }

    static void awaitConnected(final BooleanSupplier connection, final long connectionTimeoutNs, final NanoClock clock)
    {
        final long deadlineNs = clock.nanoTime() + connectionTimeoutNs;
        while (!connection.getAsBoolean())
        {
            if (clock.nanoTime() < deadlineNs)
            {
                yieldUninterruptedly();
            }
            else
            {
                throw new IllegalStateException("Failed to connect within timeout of " + connectionTimeoutNs + "ns");
            }
        }
    }

    static void yieldUninterruptedly()
    {
        Thread.yield();
        if (Thread.currentThread().isInterrupted())
        {
            throw new IllegalStateException("Interrupted while yielding...");
        }
    }
}
