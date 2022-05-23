package jekko.echonode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AeronEchoNode implements EchoNode
{
    private static Logger LOG = LoggerFactory.getLogger(AeronEchoNode.class);

    @Override
    public void run()
    {
        LOG.info("running aeron echo node!");
    }
}
