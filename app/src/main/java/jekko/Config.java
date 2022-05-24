package jekko;

import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;

final class Config
{
    private Config()
    {
    }

    // Media driver.
    static boolean embeddedMediaDriver = false;
    static String aeronDir = "";

    // Client
    static String clientEndpoint = "";
    static int clientStream = 9000;

    // Server
    static String serverEndpoint = "";
    static int serverStream = 9001;

    // Benchmark params
    static int warmUpIterations = 10;
    static int warmUpMessageRate = 20_000;
    static int iterations = 10;
    static int messageRate = 500_000;
    static int messageLength = 100;
    static int batchSize = 1;
    static int maxMessageSize = 1024;


    static IdleStrategy idleStrategy = new NoOpIdleStrategy();
    static long connectionTimeoutNs = 10_000_000_000L;
}
