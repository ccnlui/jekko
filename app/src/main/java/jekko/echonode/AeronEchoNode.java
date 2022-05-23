package jekko.echonode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;

public class AeronEchoNode implements EchoNode
{
    private static Logger LOG = LoggerFactory.getLogger(AeronEchoNode.class);

    private final MediaDriver mediaDriver;
    private final Aeron aeron;

    public AeronEchoNode(final MediaDriver mediaDriver, final Aeron aeron)
    {
        this.mediaDriver = mediaDriver;
        this.aeron = aeron;
    }

    @Override
    public void run()
    {
        LOG.info("running aeron echo node!");
    }
}
