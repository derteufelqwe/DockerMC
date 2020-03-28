package de.derteufelqwe.ServerManager.commands;

import de.derteufelqwe.ServerManager.commands.status.Proxies;
import picocli.CommandLine;

/**
 * Command to get overview information about the system
 */
@CommandLine.Command(name = "Status", description = "Shows some information about the network.",
        mixinStandardHelpOptions = true, subcommands = {
        Proxies.class
})
public class StatusCommand implements Runnable {

    @Override
    public void run() {
        System.err.println("status is no standalone command.");
        CommandLine.usage(this, System.out);
    }

}
