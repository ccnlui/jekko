package jekko;

public final class Config
{
    private Config()
    {
    }

    // Media driver.
    public static boolean embeddedMediaDriver;
    public static String aeronDir;

    // Client
    public static String clientEndpoint;
    public static int clientStream;

    // Server
    public static String serverEndpoint;
    public static int serverStream;
}
