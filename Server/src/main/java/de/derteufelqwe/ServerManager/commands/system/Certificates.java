package de.derteufelqwe.ServerManager.commands.system;

import picocli.CommandLine;

/**
 * Control over Certificates (For Registry)
 */
@CommandLine.Command(name = "certs", description = "Control the certificates.",
        mixinStandardHelpOptions = true, subcommands = {
})
public class Certificates implements Runnable {

    @Override
    public void run() {
        System.err.println("certs is no standalone command.");
        CommandLine.usage(this, System.out);
    }

}
