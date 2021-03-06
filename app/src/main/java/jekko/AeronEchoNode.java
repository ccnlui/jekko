package jekko;

import static jekko.Util.NANOS_PER_SECOND;
import static jekko.Util.aeronIpcOrUdpChannel;
import static jekko.Util.closeIfNotNull;
import static jekko.Util.connectAeron;
import static jekko.Util.launchEmbeddedMediaDriverIfConfigured;
import static jekko.Util.retryPublicationResult;

import java.nio.ByteBuffer;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.NanoClock;
import org.agrona.concurrent.ShutdownSignalBarrier;
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

class AeronEchoNode implements EchoNode, Agent
{
    private static Logger LOG = LoggerFactory.getLogger(AeronEchoNode.class);

    private final MediaDriver mediaDriver;
    private final Aeron aeron;
    private final Subscription sub;
    private final Publication pub;
    private final UnsafeBuffer inBuf;
    private final FragmentHandler assembler;

    private final NanoClock clock;
    private long nowNs;
    private long nextReportTimeNs;
    private long echoedMsg;
    private long droppedMsg;

    public AeronEchoNode()
    {
        this.mediaDriver = launchEmbeddedMediaDriverIfConfigured();
        this.aeron = connectAeron(this.mediaDriver);

        final String inChannel = aeronIpcOrUdpChannel(Config.serverEndpoint);
        final int inStream = Config.serverStream;
        final String outChannel = aeronIpcOrUdpChannel(Config.clientEndpoint);
        final int outStream = Config.clientStream;
        this.sub = aeron.addSubscription(inChannel, inStream);
        this.pub = aeron.addPublication(outChannel, outStream);

        this.inBuf = new UnsafeBuffer(ByteBuffer.allocateDirect(Config.maxMessageSize));
        this.assembler = new FragmentAssembler(this::onMessage);
        this.clock = SystemNanoClock.INSTANCE;
        this.nowNs = clock.nanoTime();
        this.nextReportTimeNs = nowNs + NANOS_PER_SECOND;

        // awaitConnected(
        //     () -> sub.isConnected(),
        //     Config.connectionTimeoutNs,
        //     SystemNanoClock.INSTANCE
        // );
        LOG.info("server: in: {}:{}", inChannel, inStream);
        LOG.info("server: out: {}:{}", outChannel, outStream);
    }

    private void onMessage(DirectBuffer buffer, int offset, int length, Header header)
    {
        if (pub.isConnected())
        {
            long result;
            while ((result = pub.offer(buffer, offset, length)) <= 0)
            {
                if (!retryPublicationResult(result))
                {
                    droppedMsg += 1;
                    LOG.error("dropped message: {}", droppedMsg);
                    return;
                }
            }
            echoedMsg += 1;

            // Debug only.
            // long timestamp = buffer.getLong(offset, ByteOrder.LITTLE_ENDIAN);
            // int messageLength = length - Long.BYTES;
            // buffer.getBytes(offset+Long.BYTES, inBuf, 0, messageLength);
            // LOG.debug("echoed {}: timestamp: {} msg: {} offset: {} length: {}",
            //     echoedMsg,
            //     timestamp,
            //     inBuf.getStringWithoutLengthAscii(0, messageLength),
            //     offset,
            //     length
            // );
        }
    }

    @Override
    public void run() throws Exception
    {
        final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();
        AgentRunner agentRunner = new AgentRunner(
            Config.idleStrategy,
            Throwable::printStackTrace,
            null,
            this
        );
        AgentRunner.startOnThread(agentRunner);

        barrier.await();
        close();
    }

    @Override
    public void close() throws Exception
    {
        closeIfNotNull(sub);
        closeIfNotNull(aeron);
        closeIfNotNull(mediaDriver);
    }

    @Override
    public int doWork() throws Exception
    {
        int fragments = sub.poll(this.assembler, 10);
        nowNs = clock.nanoTime();
        if (nextReportTimeNs <= nowNs)
        {
            LOG.info("echoed: {} dropped: {}", echoedMsg, droppedMsg);
            nextReportTimeNs += NANOS_PER_SECOND;
        }
        return fragments;
    }

    @Override
    public String roleName()
    {
        return "aeron-echo-node";
    }
}
