package jekko;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "jekko",
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true,
    description = "Marketdata message transport RTT benchmark tool"
)
public class Jekko
{
    public static void main(String[] args)
    {
        CommandLine cmd = new CommandLine(new Jekko());
        cmd.addSubcommand("server", Server.class);
        cmd.addSubcommand("client", Client.class);
        System.exit(cmd.execute(args));
    }
}
