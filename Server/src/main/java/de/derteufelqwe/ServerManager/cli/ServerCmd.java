package de.derteufelqwe.ServerManager.cli;


import de.derteufelqwe.ServerManager.cli.server.GetLobbyServerCmd;
import de.derteufelqwe.ServerManager.cli.server.ListContainersCmd;
import de.derteufelqwe.ServerManager.cli.system.*;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(name = "server", subcommands = {HelpCommand.class,
        ListContainersCmd.class, GetLobbyServerCmd.class
})
@Log4j2
public class ServerCmd implements Runnable {

    @Getter
    @CommandLine.ParentCommand
    private CliCommands parent;

    @Override
    public void run() {
        System.out.println(new CommandLine(this).setUsageHelpAutoWidth(true).getUsageMessage());
    }
}
