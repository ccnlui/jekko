package jekko.echonode;

import static jekko.Util.aeronIpcOrUdpChannel;
import static jekko.Util.closeIfNotNull;
import static jekko.Util.connectAeron;
import static jekko.Util.launchEmbeddedMediaDriverIfConfigured;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import jekko.Config;

public class AeronEchoNode implements EchoNode
{
    private static Logger LOG = LoggerFactory.getLogger(AeronEchoNode.class);

    private final MediaDriver mediaDriver;
    private final Aeron aeron;
    private Subscription sub;

    public AeronEchoNode()
    {
        this(launchEmbeddedMediaDriverIfConfigured(), connectAeron());
    }

    public AeronEchoNode(final MediaDriver mediaDriver, final Aeron aeron)
    {
        this.mediaDriver = mediaDriver;
        this.aeron = aeron;
    }

    public void init()
    {
        final String inChannel = aeronIpcOrUdpChannel(Config.serverEndpoint);
        final int inStream = 4297;
        this.sub = aeron.addSubscription(inChannel, inStream);

        LOG.info("server: in: {}:{}", inChannel, inStream);
        // LOG.info("server: out: {}:{}", outChannel, outStream);
    }

    @Override
    public void run()
    {
        LOG.info("running aeron echo node!");
    }

    @Override
    public void close() throws Exception
    {
        closeIfNotNull(sub);
        closeIfNotNull(aeron);
        closeIfNotNull(mediaDriver);
    }
}
