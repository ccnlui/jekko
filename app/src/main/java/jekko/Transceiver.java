package jekko;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.HOURS;
import java.util.concurrent.atomic.AtomicLong;

import org.HdrHistogram.Histogram;
import org.agrona.concurrent.NanoClock;
import org.agrona.concurrent.SystemNanoClock;

public abstract class Transceiver
{
    final NanoClock clock;
    final Histogram histogram;
    final AtomicLong receivedMessages = new AtomicLong(0);

    Transceiver()
    {
        this(SystemNanoClock.INSTANCE, new Histogram(HOURS.toNanos(1), 3));
    }

    Transceiver(final NanoClock clock, final Histogram histogram)
    {
        this.clock = requireNonNull(clock);
        this.histogram = requireNonNull(histogram);
    }

    public abstract int send(int numberOfMessages, int messageLength, long timestamp, long checksum);

    public abstract void receive();

    final void reset()
    {
        histogram.reset();
        receivedMessages.set(0);
    }
}
