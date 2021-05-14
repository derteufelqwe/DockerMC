package de.derteufelqwe.ServerManager.cli;


import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.cli.service.CreateServiceCmd;
import de.derteufelqwe.ServerManager.cli.service.ListServicesCmd;
import de.derteufelqwe.ServerManager.cli.service.ServiceDetailsCmd;
import de.derteufelqwe.ServerManager.cli.service.StopServiceCmd;
import de.derteufelqwe.ServerManager.cli.system.*;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(name = "service", subcommands = {HelpCommand.class,
        ListServicesCmd.class, CreateServiceCmd.class, StopServiceCmd.class, ServiceDetailsCmd.class
})
@Log4j2
public class ServiceCmd implements Runnable {

    @Getter
    @CommandLine.ParentCommand
    private CliCommands parent;

    @Override
    public void run() {
        System.out.println(new CommandLine(this).setUsageHelpAutoWidth(true).getUsageMessage());
    }
}
