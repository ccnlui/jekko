package jekko;

import static jekko.Util.aeronIpcOrUdpChannel;
import static jekko.Util.checkPublicationResult;
import static jekko.Util.closeIfNotNull;
import static jekko.Util.connectAeron;
import static jekko.Util.launchEmbeddedMediaDriverIfConfigured;
import static jekko.Util.awaitConnected;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.HdrHistogram.Histogram;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.NanoClock;
import org.agrona.concurrent.SystemNanoClock;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;

public final class AeronTransceiver extends Transceiver
{
    private static final Logger LOG = LoggerFactory.getLogger(AeronTransceiver.class);

    private final MediaDriver mediaDriver;
    private final Aeron aeron;
    private final Subscription sub;
    private final Publication pub;
    private final UnsafeBuffer inBuf;
    private final UnsafeBuffer outBuf;
    private final FragmentHandler assembler;

    public AeronTransceiver(NanoClock clock, Histogram histogram)
    {
        this(clock, histogram, launchEmbeddedMediaDriverIfConfigured(), connectAeron());
    }

    public AeronTransceiver(NanoClock clock, Histogram histogram, final MediaDriver mediaDriver, final Aeron aeron)
    {
        super(clock, histogram);
        this.mediaDriver = mediaDriver;
        this.aeron = aeron;

        final String inChannel = aeronIpcOrUdpChannel(Config.clientEndpoint);
        final int inStream = Config.clientStream;
        this.sub = aeron.addSubscription(inChannel, inStream);
        final String outChannel = aeronIpcOrUdpChannel(Config.serverEndpoint);
        final int outStream = Config.serverStream;
        this.pub = aeron.addPublication(outChannel, outStream);

        this.inBuf = new UnsafeBuffer(ByteBuffer.allocateDirect(Config.maxMessageSize));
        this.outBuf = new UnsafeBuffer(ByteBuffer.allocateDirect(Config.maxMessageSize));
        this.assembler = new FragmentAssembler(this::onMessage);

        awaitConnected(
            () -> pub.isConnected(),
            Config.connectionTimeoutNs,
            SystemNanoClock.INSTANCE
        );
        LOG.info("client: in: {}:{}", inChannel, inStream);
        LOG.info("client: out: {}:{}", outChannel, outStream);
    }

    private void onMessage(DirectBuffer buffer, int offset, int length, Header header)
    {
        final long timestamp = buffer.getLong(offset, ByteOrder.LITTLE_ENDIAN);
        histogram.recordValue(clock.nanoTime() - timestamp);
        receivedMessages.getAndIncrement();
        // Debug only.
        // final int messagePos = offset + Long.BYTES;
        // final int messageLength = length - Long.BYTES;
        // buffer.getBytes(messagePos, inBuf, 0, messageLength);
        // LOG.debug("received: timestamp: {} msg: {} offset: {} length: {}",
        //     timestamp,
        //     inBuf.getStringWithoutLengthAscii(0, messageLength),
        //     offset,
        //     length
        // );
    }

    @Override
    public int send(byte[] msg, int numberOfMessages, long timestamp)
    {
        final int messageLength = msg.length;

        // Write timestamp.
        outBuf.putLong(0, timestamp, ByteOrder.LITTLE_ENDIAN);

        // Write message.
        outBuf.putBytes(8, msg);

        int count = 0;
        for (int i = 0; i < numberOfMessages; i++)
        {
            // Debug only.
            // LOG.debug("send: timestamp: {} msg: {}",
            //     timestamp,
            //     outBuf.getStringWithoutLengthAscii(8, messageLength)
            // );

            final long result = pub.offer(outBuf, 0, Long.BYTES+messageLength);
            if (result <0)
            {
                checkPublicationResult(result);
                LOG.warn("failed to offer message: {}", Publication.errorString(result));
                break;
            }
            if (result > 0)
            {
                count += 1;
            }
        }
        return count;
    }

    @Override
    public void receive()
    {
        sub.poll(this.assembler, 10);
    }

    @Override
    public void close() throws Exception
    {
        closeIfNotNull(pub);
        closeIfNotNull(aeron);
        closeIfNotNull(mediaDriver);
    }
}
