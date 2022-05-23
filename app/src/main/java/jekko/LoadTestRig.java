package jekko;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jekko.transceiver.AeronTransceiver;
import jekko.transceiver.Transceiver;

public class LoadTestRig
{
    private static final Logger LOG = LoggerFactory.getLogger(LoadTestRig.class);

    private final Transceiver transceiver;

    public LoadTestRig(String transport) throws ExceptionInInitializerError
    {
        this.transceiver = switch (transport)
        {
            case "aeron" -> new AeronTransceiver();
            default -> throw new ExceptionInInitializerError("unexpected transport:" + transport);
        };
    }

    public void run() throws Exception
    {
        LOG.info("run load test!");
        for (int i = 0; i < 10; i++)
        {
            LOG.info("{}", System.nanoTime());
            Thread.sleep(1000);
        }

        transceiver.close();
    }
}
