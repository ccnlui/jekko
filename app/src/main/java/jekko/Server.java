package jekko;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "server", usageHelpAutoWidth = true,
    description = "Echo back every message to client")
public class Server implements Callable<Void>
{
    @Option(names = {"-h", "--help"}, usageHelp = true,
        description = "help message")
    boolean help;

    private static final Logger LOG =
        LoggerFactory.getLogger(Server.class);

    @Override
    public Void call() throws Exception
    {
        LOG.info("echo server!");
        return null;
    }
}
