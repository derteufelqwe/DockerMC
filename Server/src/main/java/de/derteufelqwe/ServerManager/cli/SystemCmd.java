package de.derteufelqwe.ServerManager.cli;


import de.derteufelqwe.ServerManager.cli.system.*;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(name = "system", subcommands = {HelpCommand.class,
        ReloadConfigCmd.class, CreateInfrastructureCmd.class, ShutdownInfrastructureCmd.class, RegistryCertInfosCmd.class,
        ClearDataCommand.class, ListNodesCmd.class, JoinNodeCmd.class
})
@Log4j2
public class SystemCmd implements Runnable {

    @Getter
    @CommandLine.ParentCommand
    private CliCommands parent;

    @Override
    public void run() {
        System.out.println(new CommandLine(this).setUsageHelpAutoWidth(true).getUsageMessage());
    }
}
