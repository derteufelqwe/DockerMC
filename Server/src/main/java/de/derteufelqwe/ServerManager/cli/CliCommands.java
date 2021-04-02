package de.derteufelqwe.ServerManager.cli;

import de.derteufelqwe.ServerManager.spring.commands.SystemCommands;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

/**
 * Top-level command that just prints help.
 */
@CommandLine.Command(name = "", subcommands = {PicocliCommands.ClearScreen.class, CommandLine.HelpCommand.class,
        SystemCmd.class
})
@Log4j2
public class CliCommands implements Runnable {

    public CliCommands() {
    }

    public void run() {
//        out.println(new CommandLine(this).getUsageMessage());
        log.error("This shouldn't be called!");
    }
}