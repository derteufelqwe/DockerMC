package de.derteufelqwe.ServerManager.commands;

import de.derteufelqwe.ServerManager.commands.docker.Containers;
import picocli.CommandLine;

@CommandLine.Command(name = "Docker", description = "Some level of control about Docker.",
        mixinStandardHelpOptions = true, subcommands = {
        Containers.class
})
public class DockerCommand implements Runnable {

    @Override
    public void run() {
        System.err.println("docker is no standalone command.");
        CommandLine.usage(this, System.out);
    }

}
