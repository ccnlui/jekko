package jekko;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicLong;

import org.HdrHistogram.Histogram;
import org.agrona.concurrent.NanoClock;

public abstract class Transceiver
{
    final NanoClock clock;
    final Histogram histogram;
    final AtomicLong receivedMessages;

    Transceiver(final NanoClock clock, final Histogram histogram)
    {
        this.clock = requireNonNull(clock);
        this.histogram = requireNonNull(histogram);
        this.receivedMessages = new AtomicLong(0);
    }

    public abstract int send(byte[] msg, int numberOfMessages, long timestamp);

    public abstract void receive();

    public abstract void close() throws Exception;

    final void reset()
    {
        receivedMessages.set(0);
    }
}
