package de.derteufelqwe.ServerManager.commands.status;

import picocli.CommandLine;

/**
 * Command to manage the BungeeCord Proxies.
 */
@CommandLine.Command(name = "proxies", description = "Commands to inspect the BungeeCord Proxies.",
        mixinStandardHelpOptions = true, subcommands = {
    ProxiesList.class
})
public class Proxies implements Runnable {

    @Override
    public void run() {
        System.err.println("proxies is no standalone command.");
        CommandLine.usage(this, System.out);
    }

}
