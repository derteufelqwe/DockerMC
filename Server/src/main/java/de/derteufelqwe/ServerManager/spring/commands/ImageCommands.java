package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.objects.CertificateCfg;
import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI;
import de.derteufelqwe.ServerManager.registry.RegistryAPIException;
import de.derteufelqwe.ServerManager.registry.objects.*;
import de.derteufelqwe.ServerManager.setup.infrastructure.RegistryCertificates;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.config.Config;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import javax.annotation.CheckForNull;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ShellComponent
@Log4j2
@ShellCommandGroup(value = "image")
public class ImageCommands {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private Docker docker;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired(required = false)
    private DockerRegistryAPI registryAPI;
    @Autowired
    private Config<MainConfig> mainConfig;


    @ShellMethod(value = "Lists all available images", key = "image list")
    public void listImages() {
        Catalog catalog = registryAPI.getCatalog();

        TableBuilder tableBuilder = new TableBuilder()
                .withColumn(new Column.Builder()
                        .withTitle("Name")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Tags")
                        .build());

        for (String image : catalog.getRepositories()) {
            Tags tags = registryAPI.getTags(image);
            List<String> firstTags = tags.getTags();
            int additionalTags = 0;

            if (tags.getTags().size() == 0)
                continue;

            // Trim the tags if there are too many
            if (tags.getTags().size() > 10) {
                firstTags = tags.getTags().subList(0, 10);
                additionalTags = tags.getTags().size() - 10;
            }

            tableBuilder.addToColumn(0, image);
            tableBuilder.addToColumn(1, String.join(", ", firstTags) + "  (" + additionalTags + " more)");
        }

        tableBuilder.build(log);
    }

    @ShellMethod(value = "Lists all tags for an image.", key = "image tags")
    public void imageTags(String imageName) {
        Tags tags;
        try {
            tags = registryAPI.getTags(imageName);
            if (tags.getTags().size() == 0)
                throw new RegistryAPIException("");

        } catch (RegistryAPIException e) {
            log.error("Image '{}' not found in registry.", imageName);
            return;
        }

        TableBuilder tableBuilder = new TableBuilder()
                .withColumn(new Column.Builder()
                        .withTitle("Tag")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Last modified")
                        .build());

        for (String tag : tags.getTags()) {
            ImageManifest manifest = registryAPI.getManifest(imageName, tag);

            tableBuilder.addToColumn(0, tag);
            tableBuilder.addToColumn(1, manifest.getLastModified().toString());
        }

        tableBuilder.build(log);
    }

    @ShellMethod(value = "Deletes an image from the registry.", key = "image delete")
    public void deleteImage(String image, @ShellOption(value = {"-t", "--tag"}, defaultValue = "") String tag, @ShellOption({"-a", "--all"}) boolean all) {
        if (tag.equals("") && !all) {
            log.error("Specify --tag [tag] or --all");
            return;
        }

        // Check if actually present
        try {
            Tags tags = registryAPI.getTags(image);
            if (!all && !tags.getTags().contains(tag)) {
                log.error("Image '{}' has no tag '{}'.", image, tag);
                return;
            }

        } catch (RegistryAPIException e) {
            log.error("Image '{}' not found in registry.", image);
            return;
        }

        List<String> command = new ArrayList<>(Arrays.asList(
                "-registry", "https://registry:5000", "-username", mainConfig.get().getRegistryUsername(),
                "-password", mainConfig.get().getRegistryPassword(), "-repos", image, "-latest", "0", "-insecure"
        ));

        if (!all) {
            command.add("-tag_regexp");
            command.add(tag);
        }

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
                .awaitStatusCode(20, TimeUnit.SECONDS);

        if (exitCode != 0) {
            log.error("Failed to delete from image {}. Container {} exited with code {}.", image, response.getId(), exitCode);
            return;
        }

        if (all) {
            log.info("Removed image {} from the registry.", image);
        } else {
            log.info("Removed tag {} of image {} from the registry.", tag, image);
        }
    }

    @ShellMethod(value = "Builds a new image for use in the swarm.", key = "image build")
    public void buildImage(String imageType, String name, String tag) {
        ImageType type = parseImageType(imageType);
        if (type == null) {
            log.error("Invalid type {}.", imageType);
            return;
        }

        // Create the required files
        File dockerfileIn;
        File dockerfileOut;
        if (type == ImageType.MINECRAFT) {
            dockerfileIn = new File(Constants.DOCKERFILE_MINECRAFT_PATH);
            dockerfileOut = new File(Constants.IMAGE_MINECRAFT_PATH + name + "/Dockerfile");

        } else {
            dockerfileIn = new File(Constants.DOCKERFILE_BUNGEE_PATH);
            dockerfileOut = new File(Constants.IMAGE_BUNGEE_PATH + name + "/Dockerfile");
        }

        // Temporarily copy the dockerfile
        try {
            FileUtils.copyFile(dockerfileIn, dockerfileOut);

        } catch (Exception e) {
            log.error("Failed to copy Dockerfile. Error: {}.", e.getMessage());
            return;
        }



        // Remove the dockerfile again
        dockerfileOut.delete();

        System.out.println("Done");
    }

    @CheckForNull
    private ImageType parseImageType(String imageType) {
        imageType = imageType.toUpperCase();
        if (imageType.equals("MC"))
            imageType = "MINECRAFT";
        if (imageType.equals("BC"))
            imageType = "BUNGEE";

        try {
            return ImageType.valueOf(imageType);

        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    /*
     *  docker run --rm --network overnet lhanxetus/deckschrubber -registry https://registry:5000 -username admin -password root -repos testmc -latest 0 -debug -insecure
     *  openssl x509 -in ca.crt -noout -text
     */

    @SneakyThrows
    @ShellMethod(value = "testing", key = "test")
    public void test() {
        buildImage("MC", "testmc", "latest");
    }


    public enum ImageType {
        MINECRAFT,
        BUNGEE;
    }

}
