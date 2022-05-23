package jekko;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jekko.transceiver.Transceiver;

public class LoadTestRig
{
    private static final Logger LOG = LoggerFactory.getLogger(LoadTestRig.class);

    private final Transceiver transceiver;

    public LoadTestRig(Transceiver transceiver)
    {
        this.transceiver = transceiver;
    }

    public void run() throws InterruptedException
    {
        LOG.info("run load test!");
        while (true)
        {
            LOG.info("{}", System.nanoTime());
            Thread.sleep(1000);
        }
    }
}
