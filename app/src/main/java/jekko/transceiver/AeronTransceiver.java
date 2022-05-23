package jekko.transceiver;

import static jekko.Util.aeronIpcOrUdpChannel;
import static jekko.Util.closeIfNotNull;
import static jekko.Util.connectAeron;
import static jekko.Util.launchEmbeddedMediaDriverIfConfigured;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import jekko.Config;

public final class AeronTransceiver extends Transceiver
{
    private static final Logger LOG = LoggerFactory.getLogger(AeronTransceiver.class);

    private final MediaDriver mediaDriver;
    private final Aeron aeron;
    private Publication pub;

    public AeronTransceiver()
    {
        this(launchEmbeddedMediaDriverIfConfigured(), connectAeron());
    }

    public AeronTransceiver(final MediaDriver mediaDriver, final Aeron aeron)
    {
        this.mediaDriver = mediaDriver;
        this.aeron = aeron;
    }

    public void init()
    {
        final String outChannel = aeronIpcOrUdpChannel(Config.serverEndpoint);
        final int outStream = Config.serverStream;
        this.pub = aeron.addPublication(outChannel, outStream);

        // LOG.info("client: in: {}:{}", inChannel, inStream);
        LOG.info("client: out: {}:{}", outChannel, outStream);
    }

    @Override
    public int send(int numberOfMessages, int messageLength, long timestamp, long checksum)
    {
        return 0;
    }

    @Override
    public void receive()
    {   
    }

    @Override
    public void close() throws Exception
    {
        closeIfNotNull(pub);
        closeIfNotNull(aeron);
        closeIfNotNull(mediaDriver);    
    }
}
