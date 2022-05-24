package jekko;

import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "client", usageHelpAutoWidth = true,
    description = "Send messages to echo server, measure RTT latencies in microseconds")
public class Client implements Callable<Void>
{
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "help message")
    boolean help;

    @Option(names = "--embedded-media-driver", defaultValue = "false",
        description = "launch with embedded media driver (default ${DEFAULT-VALUE})")
    boolean embeddedMediaDriver;

    @Option(names = "--aeron-dir", defaultValue = "",
        description = "override directory name for embedded aeron media driver")
    String aeronDir;

    @Option(names = "--pub-endpoint", defaultValue = "",
        description = "aeron udp transport endpoint to which messages are published in address:port format (default: \"${DEFAULT-VALUE}\")")
    String pubEndpoint;

    @Option(names = "--sub-endpoint", defaultValue = "",
        description = "aeron udp transport endpoint from which messages are subscribed in address:port format (default: \"${DEFAULT-VALUE}\")")
    String subEndpoint;

    @Override
    public Void call() throws Exception
    {
        mergeConfig();
        Util.setLogbackLoggerLevel(Config.logLevel);
        new LoadTestRig("aeron").run();
        return null;
    }

    private void mergeConfig()
    {
        if (this.embeddedMediaDriver)
        {
            Config.embeddedMediaDriver = this.embeddedMediaDriver;
        }
        if (this.aeronDir != null && !this.aeronDir.isEmpty())
        {
            Config.aeronDir = this.aeronDir;
        }
        if (this.pubEndpoint != null && !this.pubEndpoint.isEmpty())
        {
            Config.serverEndpoint = this.pubEndpoint;
        }
        if (this.subEndpoint != null && !this.subEndpoint.isEmpty())
        {
            Config.clientEndpoint = this.subEndpoint;
        }
    }
}
