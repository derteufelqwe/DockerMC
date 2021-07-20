package de.derteufelqwe.ServerManager.cli;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jline.reader.LineReader;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

/**
 * Top-level command that just prints help.
 */
@CommandLine.Command(name = "", subcommands = {PicocliCommands.ClearScreen.class, HelpCommand.class,
        SystemCmd.class, ServiceCmd.class, ServerCmd.class, ImageCmd.class
})
@Log4j2
public class CliCommands implements Runnable {

    @Getter
    @Setter
    private LineReader lineReader;

    public CliCommands() {

    }

    public void run() {
        log.error("This shouldn't be called!");
    }
}