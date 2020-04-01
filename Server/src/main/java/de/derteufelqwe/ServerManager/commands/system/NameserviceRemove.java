package de.derteufelqwe.ServerManager.commands.system;

import de.derteufelqwe.ServerManager.setup.BindConfigurator;
import picocli.CommandLine;

/**
 * Remove entries from the DNS list
 */
@CommandLine.Command(name = "rm", aliases = {"remove"}, description = "Remove entries from the user defined dns record.",
        mixinStandardHelpOptions = true, subcommands = {

})
public class NameserviceRemove implements Runnable {

    @CommandLine.Parameters(index = "0", description = "Record name")
    private String dnsToRemove;

    private BindConfigurator bindConfigurator = new BindConfigurator(true);

    @Override
    public void run() {
        boolean result = bindConfigurator.removeEntry(BindConfigurator.Type.USER, dnsToRemove);

        if (!result) {
            System.out.println("Couldn't find record matching " + dnsToRemove);

        } else {
            System.out.println("Removed " + dnsToRemove + " from user defined entries.");
        }
    }

}
