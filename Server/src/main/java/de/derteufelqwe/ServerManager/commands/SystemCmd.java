package de.derteufelqwe.ServerManager.commands;

import de.derteufelqwe.ServerManager.commands.system.Certificates;
import picocli.CommandLine;

/**
 * Controls core functionalities of the system
 */
@CommandLine.Command(name = "System", description = "Control core functions of the system",
        mixinStandardHelpOptions = true, subcommands = {
        Certificates.class
})
public class SystemCmd implements Runnable {

    @Override
    public void run() {
        System.err.println("system is no standalone command.");
        CommandLine.usage(this, System.out);
    }

}
