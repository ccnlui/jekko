package jekko.transceiver;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;

public final class AeronTransceiver extends Transceiver
{
    private final MediaDriver mediaDriver;
    private final Aeron aeron;

    public AeronTransceiver(final MediaDriver mediaDriver, final Aeron aeron, final boolean embeddedMediaDriver)
    {
        this.mediaDriver = mediaDriver;
        this.aeron = aeron;
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
}
