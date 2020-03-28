package de.derteufelqwe.ServerManager.commands.system;

import de.derteufelqwe.ServerManager.ContainerGetter;
import de.derteufelqwe.ServerManager.ServerManager;
import picocli.CommandLine;

/**
 * Reload the DNS entries
 */
@CommandLine.Command(name = "reload", description = "Reloads the DNS server",
        mixinStandardHelpOptions = true, subcommands = {

})
public class NameserviceReload implements Runnable {

    @CommandLine.Option(names = {"-f", "--force"}, description = "Enforce the reload")
    private boolean force = false;


    @Override
    public void run() {
        String command = "reload";
        if (force) {
            command = "force-reload";
        }
        String dnsContainerID = new ContainerGetter().getDNSContainer().getId();

        ServerManager.getDocker().execContainer(dnsContainerID, "/etc/init.d/bind9", command);

        System.out.println("Reloaded DNS server.");
    }

}
