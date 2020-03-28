package de.derteufelqwe.ServerManager.commands.system;

import de.derteufelqwe.ServerManager.setup.BindConfigurator;
import picocli.CommandLine;

/**
 * Add entries to the DNS list
 */
@CommandLine.Command(name = "add", description = "Add entries to the user defined dns record.",
        mixinStandardHelpOptions = true, subcommands = {

})
public class NameserviceAdd implements Runnable {

    @CommandLine.Parameters(index = "0", description = "Record name")
    private String dnsToAdd;

    @CommandLine.Parameters(index = "1", description = "IP")
    private String ipToAdd;

    private BindConfigurator bindConfigurator = new BindConfigurator();

    @Override
    public void run() {
        bindConfigurator.addEntry(BindConfigurator.Type.USER, dnsToAdd, ipToAdd);
        System.out.println("Added " + dnsToAdd + " to user defined records.");
    }

}
