package de.derteufelqwe.ServerManager.spring.commands;

import de.derteufelqwe.ServerManager.utils.HelpBuilder;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.commands.Help;

@ShellComponent
public class HelpCommand implements Help.Command {


    /**
     * Custom help method. Doesn't use logger as this doesn't need to be logged
     */
    @ShellMethod("Prints the help")
    public void help() {
        System.out.println("DockerMC ServerManager help");
        System.out.println("");
        System.out.println("Usage: [command] [subcommand, help]");
        System.out.println("");

        new HelpBuilder("Default Commands:")
                .addEntry("help", "Shows this help.")
                .addEntry("history", "Shows the command history.")
                .addEntry("exit", "Exits the DockerMC console. This doesn't affect the servers.")
                .addEntry("clear", "Clears the terminal.")
                .print();

        System.out.println("");
        new HelpBuilder("DockerMC Commands:")
                .addEntry("image", "Manage Minecraft and BungeeCord images")
                .addEntry("system", "Manage the DockerMC system")
                .addEntry("server", "Manage the individual MC / BC servers")
                .addEntry("server", "Manage the individual MC / BC services")
                .print();

        System.out.println("");
        System.out.println("Run COMMAND --help for more information on a command.");
    }

}
