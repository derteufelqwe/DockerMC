package de.derteufelqwe.ServerManager.cli.image;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI;
import de.derteufelqwe.ServerManager.registry.RegistryAPIException;
import de.derteufelqwe.ServerManager.registry.objects.ImageManifest;
import de.derteufelqwe.ServerManager.registry.objects.Tags;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CommandLine.Command(name = "delete", aliases = {"remove", "rm"}, description = "Removes versions of images")
@Log4j2
public class DeleteImageCmd implements Runnable {

    private Docker docker = ServerManager.getDocker();
    private DockerRegistryAPI registryAPI = ServerManager.getRegistryAPI();
    private Config<MainConfig> mainConfig = ServerManager.getMainConfig();


    @CommandLine.Option(names = {"-t", "--tag"}, description = "Tags to remove")
    private List<String> tags;

    @CommandLine.Option(names = {"-a", "--all"}, description = "Remove all versions of the image")
    private boolean all = false;

    @CommandLine.Parameters(description = "Name of the image")
    private String image;


    @Override
    public void run() {
        if ((tags == null || tags.size() == 0) && !all) {
            log.error("Specify --tags [tag] or --all");
            return;
        }

        List<String> availableTags;
        try {
            availableTags = getAvailableTags();

        } catch (RegistryAPIException e) {
            log.error("Image '{}' not found in registry.", image);
            return;
        }

        if (!all && availableTags.size() == 0) {
            log.warn("None of the supplied tags exists.");
            return;
        }

        List<String> command = createCommand(availableTags);

        // Run the docker container
        CreateContainerResponse response = docker.getDocker().createContainerCmd(Constants.Images.DECKSCHRUBBER.image())
                .withCmd(command)
                .exec();

        log.debug("Image delete container: {}.", response.getId());

        docker.getDocker().connectToNetworkCmd()
                .withContainerId(response.getId())
                .withNetworkId(Constants.NETW_OVERNET_NAME)
                .exec();

        docker.getDocker().startContainerCmd(response.getId()).exec();

        // Check if the container finished properly
        int exitCode = docker.getDocker().waitContainerCmd(response.getId())
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode(60, TimeUnit.SECONDS);

        if (exitCode != 0) {
            log.error("Failed to delete from image {}. Container {} exited with code {}.", image, response.getId(), exitCode);
            return;
        }

        if (all) {
            log.info("Removed image {} from the registry.", image);
        } else {
            log.info("Removed tags {} of image {} from the registry.", tags, image);
        }
    }


    @NotNull
    private List<String> getAvailableTags() throws RegistryAPIException {
        List<String> availableTags = new ArrayList<>();

        if (all) {
            return availableTags;
        }

        Tags dockerTags = registryAPI.getTags(image);

        for (String tag : tags) {
            if (!dockerTags.getTags().contains(tag)) {
                log.warn("Image '{}' has no tag '{}'. Ignoring it", image, tag);

            } else {
                availableTags.add(tag);
            }
        }

        return availableTags;
    }

    @NotNull
    private List<String> createCommand(List<String> availableTags) {
        List<String> command = new ArrayList<>(Arrays.asList(
                "-registry", String.format("https://%s:%s", Constants.REGISTRY_CONTAINER_NAME, Constants.REGISTY_CONTAINER_DEFAULT_PORT),
                "-username", mainConfig.get().getRegistryUsername(),
                "-password", mainConfig.get().getRegistryPassword(), "-repos", image, "-latest", "0", "-insecure"
        ));

        if (!all) {
            command.add("-tag_regexp");
            command.add(availableTags.stream()
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"))
            );
        }

        return command;
    }

}
