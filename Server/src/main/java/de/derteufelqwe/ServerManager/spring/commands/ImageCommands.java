package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectTaskCmd;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.*;
import com.google.gson.Gson;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.callbacks.ImageBuildCallback;
import de.derteufelqwe.ServerManager.callbacks.ImagePushCallback;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI;
import de.derteufelqwe.ServerManager.registry.RegistryAPIException;
import de.derteufelqwe.ServerManager.registry.objects.Catalog;
import de.derteufelqwe.ServerManager.registry.objects.ImageManifest;
import de.derteufelqwe.ServerManager.registry.objects.Tags;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.ServerManager.utils.HelpBuilder;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.units.qual.A;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import javax.annotation.CheckForNull;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

@ShellComponent
@Log4j2
public class ImageCommands {

    private final Pattern RE_IMAGE_NAME = Pattern.compile("[a-z0-9]+(?:[._-]{1,2}[a-z0-9]+)*");
    private final Pattern RE_IMAGE_TAG = Pattern.compile("[a-z0-9]+(?:[a-z0-9]+)*");

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
    @Autowired
    private Gson gson;


    @ShellMethod(value = "Shows the help", key = "image")
    public void showHelp() {
        System.out.println("Manage images");
        System.out.println("");

        new HelpBuilder("Commands:")
                .addEntry("help", "Shows this help")
                .addEntry("list", "Lists all available images in the local registry")
                .addEntry("tags", "Lists all tags an image has")
                .addEntry("delete", "Deletes an image from the registry (experimental!)")
                .addEntry("build", "Builds a new image and pushed it to the registry")
                .print();
    }

    @ShellMethod(value = "Shows the help", key = "image help")
    public void showHelp2() {
        showHelp();
    }


    @ShellMethod(value = "Lists all available images", key = "image list")
    public void listImages(@ShellOption({"-nb", "--noBungee"}) boolean noBungee, @ShellOption({"-nm", "--noMinecraft"}) boolean noMinecraft) {
        Catalog catalog = registryAPI.getCatalog();

        TableBuilder tableBuilder = new TableBuilder()
                .withNoRowSeparation()
                .withColumn(new Column.Builder()
                        .withTitle("Name")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Type")
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

            // Skip images based on the filter
            ImageManifest manifest = registryAPI.getManifest(image, firstTags.get(0));
            String type = manifest.getDMCImageType();

            if (noMinecraft && type != null && type.equals(ImageType.MINECRAFT.name()))
                continue;
            if (noBungee && type != null && type.equals(ImageType.BUNGEE.name()))
                continue;

            tableBuilder.addToColumn(0, image);
            tableBuilder.addToColumn(1, type);
            tableBuilder.addToColumn(2, String.join(", ", firstTags) + "  (" + additionalTags + " more)");
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
    public void buildImage(ImageType imageType, String name, @ShellOption(defaultValue = "latest") String tag) {
        if (imageType == null) {
            log.error("Got Invalid value for 'imageType'. Expected MINECRAFT or BUNGEE.");
            return;
        }

        name = name.toLowerCase();
        tag = tag.toLowerCase();

        // Check if name and tag are valid.
        if (!RE_IMAGE_NAME.matcher(name).matches()) {
            log.error("Invalid image name. It must follow this regex: {}", RE_IMAGE_NAME.pattern());
            return;
        }
        if (!RE_IMAGE_TAG.matcher(tag).matches()) {
            log.error("Invalid image tag. It must follow this regex: {}", RE_IMAGE_TAG.pattern());
            return;
        }
        if (name.startsWith(Constants.REGISTRY_URL) || name.contains("/")) {
            log.error("Invalid image name. It can't start with '{}' or container slashes.", Constants.REGISTRY_URL);
            return;
        }

        String fullName = Constants.REGISTRY_URL + "/" + name + ":" + tag;

        log.info("Building image {}:{}.", name, tag);
        String imageID = this.buildImage(imageType, name, tag, fullName);
        if (imageID == null)
            return;

        this.pushImage(fullName);

        log.info("Successfully build and pushed image {}:{} ({}).", name, tag, imageID);
    }

    @SneakyThrows
    @CheckForNull
    private String buildImage(ImageType type, String name, String tag, String fullName) {
        File dockerfileIn;
        File dockerfileOut;
        File sourceFolder;
        if (type == ImageType.MINECRAFT) {
            dockerfileIn = new File(Constants.DOCKERFILE_MINECRAFT_PATH);
            dockerfileOut = new File(Constants.IMAGE_MINECRAFT_PATH + name + "/Dockerfile");
            sourceFolder = new File(Constants.IMAGE_MINECRAFT_PATH + name + "/");

        } else {
            dockerfileIn = new File(Constants.DOCKERFILE_BUNGEE_PATH);
            dockerfileOut = new File(Constants.IMAGE_BUNGEE_PATH + name + "/Dockerfile");
            sourceFolder = new File(Constants.IMAGE_BUNGEE_PATH + name + "/");
        }

        // Check if the folder actually exists
        if (!sourceFolder.exists()) {
            log.error("Couldn't find image data folder {}.", name);
            return null;
        }

        // Check if the DockerMC plugin is present
        if (!checkMinecraftPlugins(sourceFolder)) {
            log.error("Image {} has no DockerMC plugin.", name);
            return null;
        }

        // Temporarily copy the dockerfile
        try {
            FileUtils.copyFile(dockerfileIn, dockerfileOut);

        } catch (Exception e) {
            log.error("Failed to copy Dockerfile. Error: {}.", e.getMessage());
            return null;
        }

        // Build the image
        BuildImageResultCallback callback = docker.getDocker().buildImageCmd(sourceFolder)
                .withTags(Collections.singleton(fullName))
                .withLabels(Collections.singletonMap(Constants.DOCKER_IMAGE_TYPE_TAG, type.name()))
                .exec(new ImageBuildCallback())
                .awaitCompletion();

        // Fetch the ID
        String imageID;
        try {
            imageID = callback.awaitImageId();
        } catch (DockerClientException e) {
            log.error("Building image {}:{} failed with: {}.", name, tag, e.getMessage());
            return null;
        }

        // Remove the dockerfile again
        dockerfileOut.delete();

        return imageID;
    }

    @SneakyThrows
    private boolean pushImage(String imageName) {
        docker.getDocker().pushImageCmd(imageName)
                .withAuthConfig(new AuthConfig()
                        .withUsername(mainConfig.get().getRegistryUsername())
                        .withPassword(mainConfig.get().getRegistryPassword()))
                .exec(new ImagePushCallback())
                .awaitCompletion()
                .awaitSuccess();

        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean checkMinecraftPlugins(File sourceFolder) {
        File pluginsPath = new File(sourceFolder.getPath() + "/plugins/");
        if (!pluginsPath.exists() || !pluginsPath.isDirectory())
            return false;

        for (File plugin : pluginsPath.listFiles()) {
            if (!plugin.isFile())
                continue;

            try {
                JarFile jarFile = new JarFile(plugin);

                try {
                    ZipEntry dockermcFile = jarFile.getEntry("dockermc.json");
                    if (dockermcFile == null)   // File not found
                        continue;
                    Map<String, Object> content = (Map<String, Object>) gson.fromJson(new InputStreamReader(jarFile.getInputStream(dockermcFile)), Map.class);
                    String type = (String) content.getOrDefault("type", "");
                    if (type.equals("MINECRAFT") || type.equals("BUNGEECORD"))
                        return true;

                } finally {
                    jarFile.close();
                }

            } catch (IOException e) {
                log.warn("IOException occurred while analyzing plugins. Message: {}", e.getMessage());

            }
        }

        return false;
    }


    @SneakyThrows
    @ShellMethod(value = "testing", key = "test")
    public void test() {


        System.out.println("done");
    }


    public enum ImageType {
        MINECRAFT,
        BUNGEE;
    }

}
