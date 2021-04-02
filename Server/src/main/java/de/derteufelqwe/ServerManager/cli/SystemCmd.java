package de.derteufelqwe.ServerManager.cli;


import de.derteufelqwe.ServerManager.cli.system.CreateInfrastructureCmd;
import de.derteufelqwe.ServerManager.cli.system.ReloadConfigCmd;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(name = "system", subcommands = {CommandLine.HelpCommand.class,
        ReloadConfigCmd.class, CreateInfrastructureCmd.class
})
@Log4j2
public class SystemCmd implements Runnable {

    @Override
    public void run() {
        System.out.println(new CommandLine(this).getUsageMessage());
    }
}
