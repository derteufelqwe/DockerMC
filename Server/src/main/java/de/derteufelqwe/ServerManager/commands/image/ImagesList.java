package de.derteufelqwe.ServerManager.commands.image;


import com.github.dockerjava.api.model.Image;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lists all Images in the registry
 */
@CommandLine.Command(name = "list", description = "Lists all images in the registry.",
        mixinStandardHelpOptions = true, subcommands = {

})
public class ImagesList implements Runnable {

    private final Pattern REGISTRY_TAG = Pattern.compile("^registry\\.swarm\\/");

    private Docker docker;

    public ImagesList() {
        this.docker = ServerManager.getDocker();
    }

    /**
     * Checks the Tags of an image if one of them starts with "registry.swarm"
     * @return Yes or no
     */
    private boolean checkTags(String[] tags) {
        if (tags == null)
            return false;

        List<String> tagList = Arrays.asList(tags);

        for (String tag : tagList) {
            if (REGISTRY_TAG.matcher(tag).find()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void run() {
        System.out.println("[Note] Listing images of registry is not implemented yet!");

        List<Image> images = docker.getDocker().listImagesCmd()
                .withShowAll(true)
                .exec();

        List<Image> registryOnlyImages = images.stream()
                .filter(i -> this.checkTags(i.getRepoTags()))
                .collect(Collectors.toList());

        System.out.println("Local images from registry:");

        for (Image image : registryOnlyImages) {
            System.out.println(image.getRepoTags()[0]);
        }

    }

}
