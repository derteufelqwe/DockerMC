package de.derteufelqwe.ServerManager.commands.docker;

import picocli.CommandLine;

@CommandLine.Command(name = "containers", description = "Information about the currently running container.",
        mixinStandardHelpOptions = true, subcommands = {
        ContainersList.class
})
public class Containers implements Runnable {

    @Override
    public void run() {
        System.err.println("containers is no standalone command.");
        CommandLine.usage(this, System.out);
    }


}
