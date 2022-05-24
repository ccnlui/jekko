package jekko;

import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static jekko.Util.NANOS_PER_SECOND;
import static jekko.Util.RECEIVE_DEADLINE_NS;

import java.util.concurrent.atomic.AtomicLong;

import org.HdrHistogram.Histogram;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NanoClock;
import org.agrona.concurrent.SystemNanoClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LoadTestRig
{
    private static final Logger LOG = LoggerFactory.getLogger(LoadTestRig.class);

    private final NanoClock clock;
    private final Histogram histogram;
    private final Transceiver transceiver;

    public LoadTestRig(String transport) throws ExceptionInInitializerError
    {
        this.clock = SystemNanoClock.INSTANCE;
        this.histogram = new Histogram(HOURS.toNanos(1), 3);
        this.transceiver = switch (transport)
        {
            case "aeron" -> new AeronTransceiver(this.clock, this.histogram);
            default -> throw new ExceptionInInitializerError("unexpected transport:" + transport);
        };
    }

    void run() throws Exception
    {
        byte[] msg = generateMessage(Config.messageLength);

        LOG.info("Running warmup for {} iterations of {} messages each, with {} bytes payload and a burst size of {}...",
            Config.warmUpIterations,
            Config.warmUpMessageRate,
            Config.messageLength,
            Config.batchSize
        );
        sendAndReceive(msg, Config.warmUpIterations, Config.warmUpMessageRate);
        histogram.reset();
        transceiver.reset();

        LOG.info("Running measurement for {} iterations of {} messages each, with {} bytes payload and a burst size of {}...",
            Config.iterations,
            Config.messageRate,
            Config.messageLength,
            Config.batchSize
        );
        long sentMessages = sendAndReceive(msg, Config.iterations, Config.messageRate);

        LOG.info("Histogram of RTT latencies in microseconds");
        histogram.outputPercentileDistribution(System.out, 1000.0);  // output in us

        warnIfTargetRateNotAchieved(sentMessages);

        transceiver.close();
    }

    public long sendAndReceive(final byte[] msg, final int iterations, final int numberOfMessages)
    {
        final Transceiver messageTransceiver = this.transceiver;
        final NanoClock clock = this.clock;
        final AtomicLong receivedMessages = messageTransceiver.receivedMessages;
        final int burstSize = Config.batchSize;
        final IdleStrategy idleStrategy = Config.idleStrategy;
        final long sendIntervalNs = NANOS_PER_SECOND * burstSize / numberOfMessages;
        final long totalNumberOfMessages = (long)iterations * numberOfMessages;

        final long startTimeNs = clock.nanoTime();
        final long endTimeNs = startTimeNs + iterations * NANOS_PER_SECOND;
        long sentMessages = 0;
        long timestampNs = startTimeNs;
        long nowNs = startTimeNs;
        long nextReportTimeNs = startTimeNs + NANOS_PER_SECOND;

        int batchSize = (int)min(totalNumberOfMessages, burstSize);
        while (true)
        {
            final int sent = messageTransceiver.send(msg, batchSize, timestampNs);

            sentMessages += sent;
            if (totalNumberOfMessages == sentMessages)
            {
                reportProgress(startTimeNs, nowNs, sentMessages);
                break;
            }

            nowNs = clock.nanoTime();
            if (sent == batchSize)
            {
                batchSize = (int)min(totalNumberOfMessages - sentMessages, burstSize);
                timestampNs += sendIntervalNs;
                if (nowNs < timestampNs && nowNs < endTimeNs)
                {
                    idleStrategy.reset();
                    long received = 0;
                    do
                    {
                        if (received < sentMessages)
                        {
                            messageTransceiver.receive();
                            final long updatedReceived = receivedMessages.get();
                            if (updatedReceived == received)
                            {
                                idleStrategy.idle();
                            }
                            else
                            {
                                received = updatedReceived;
                                idleStrategy.reset();
                            }
                        }
                        else
                        {
                            idleStrategy.idle();
                        }
                        nowNs = clock.nanoTime();
                    }
                    while (nowNs < timestampNs && nowNs < endTimeNs);
                }
            }
            else
            {
                batchSize -= sent;
                messageTransceiver.receive();
            }

            if (nowNs >= endTimeNs)
            {
                break;
            }

            if (nowNs >= nextReportTimeNs)
            {
                final int elapsedSeconds = reportProgress(startTimeNs, nowNs, sentMessages);
                nextReportTimeNs = startTimeNs + (elapsedSeconds + 1) * NANOS_PER_SECOND;
            }
        }

        idleStrategy.reset();
        long received = receivedMessages.get();
        final long deadline = clock.nanoTime() + RECEIVE_DEADLINE_NS;
        while (received < sentMessages)
        {
            messageTransceiver.receive();
            final long updatedReceived = receivedMessages.get();
            if (updatedReceived == received)
            {
                idleStrategy.idle();
                if (clock.nanoTime() >= deadline)
                {
                    LOG.warn("*** WARNING: Not all messages were received after {}s deadline!",
                        NANOSECONDS.toSeconds(RECEIVE_DEADLINE_NS));
                    break;
                }
            }
            else
            {
                received = updatedReceived;
                idleStrategy.reset();
            }
        }

        return sentMessages;
    }

    private int reportProgress(final long startTimeNs, final long nowNs, final long sentMessages)
    {
        final int elapsedSeconds = (int)round((double)(nowNs - startTimeNs) / NANOS_PER_SECOND);
        final long sendRate = 0 == elapsedSeconds ? sentMessages : sentMessages / elapsedSeconds;
        LOG.info("Send rate {} msg/sec", sendRate);

        return elapsedSeconds;
    }

    private byte[] generateMessage(final int messageLength)
    {
        byte[] buf = new byte[messageLength];
        for (int i = 0; i < messageLength; i++)
        {
            buf[i] = (byte) '1';
        }
        return buf;
    }

    private void warnIfTargetRateNotAchieved(final long sentMessages)
    {
        final long expectedTotalNumberOfMessages = Config.iterations * Config.messageRate;
        if (sentMessages < expectedTotalNumberOfMessages)
        {
            LOG.warn("*** WARNING: Target message rate not achieved: expected to send {} messages in total but managed to send only {} messages!",
                expectedTotalNumberOfMessages,
                sentMessages
            );
        }
    }
}
