package jekko;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "client", usageHelpAutoWidth = true,
    description = "Send messages to echo server, measure RTT latencies in microseconds")
public class Client implements Callable<Void>
{
    @Option(names = {"-h", "--help"}, usageHelp = true,
        description = "help message")
    boolean help;

    private static final Logger LOG =
        LoggerFactory.getLogger(Client.class);

    @Override
    public Void call() throws Exception
    {
        LOG.info("echo client!");
        return null;
    }
}
