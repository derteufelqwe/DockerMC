package de.derteufelqwe.ServerManager.commands;

import lombok.Getter;
import picocli.CommandLine;


@CommandLine.Command(name = "Exit", description = "Shuts the ServerManager down.",
        mixinStandardHelpOptions = true)
public class ExitCommand implements Runnable {

    @Getter
    @CommandLine.Option(names = {"-k", "--kill"}, description = "Kill all running containers on shutdown.")
    private boolean killContainers = false;



    @Override
    public void run() {
        System.out.println("Shutting down...");

        if (killContainers) {
            System.out.println("Killing containers not implemented yet.");
        }

    }

}
