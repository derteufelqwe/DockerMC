package de.derteufelqwe.ServerManager.commands.system;

import picocli.CommandLine;

/**
 * Control over the DNS Entrys
 */
@CommandLine.Command(name = "dns", description = "Control the userdefined DNS entrys.",
        mixinStandardHelpOptions = true, subcommands = {
    NameserviceList.class, NameserviceAdd.class, NameserviceRemove.class, NameserviceReload.class
})
public class Nameservices implements Runnable {

    @Override
    public void run() {
        System.err.println("dns is no standalone command.");
        CommandLine.usage(this, System.out);
    }

}
