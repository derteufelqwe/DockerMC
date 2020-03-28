package de.derteufelqwe.ServerManager.commands;

import de.derteufelqwe.ServerManager.commands.image.ImageBuild;
import de.derteufelqwe.ServerManager.commands.image.ImagesList;
import picocli.CommandLine;

/**
 * Command to create images
 */
@CommandLine.Command(name = "Images", description = "Command to work with building images.",
        mixinStandardHelpOptions = true, subcommands = {
        ImagesList.class, ImageBuild.class
})
public class ImageCmd implements Runnable {


    public ImageCmd() {
    }

    @Override
    public void run() {
        System.err.println("Image is no standalone command.");
        CommandLine.usage(this, System.out);
    }


}