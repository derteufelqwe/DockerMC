package de.derteufelqwe.ServerManager.cli.image;

import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.AuthConfig;
import com.google.gson.Gson;
import com.google.inject.Inject;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.callbacks.ImageBuildCallback;
import de.derteufelqwe.ServerManager.callbacks.ImagePushCallback;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

@CommandLine.Command(name = "build", description = "Build new images")
@Log4j2
public class BuildImageCmd implements Runnable {

    private final Pattern RE_IMAGE_NAME = Pattern.compile("[a-z0-9]+(?:[._-]{1,2}[a-z0-9]+)*");
    private final Pattern RE_IMAGE_TAG = Pattern.compile("[a-z0-9]+(?:[a-z0-9]+)*");


    @Inject private Docker docker;
    @Inject private Config<MainConfig> mainConfig;
    private Gson gson = new Gson();


    @CommandLine.Parameters(description = "Type of the image. MC or BC")
    private ImageType type;

    @CommandLine.Parameters(description = "Name of the image")
    private String imageName;

    @CommandLine.Option(names = {"-t", "--tag"}, description = "The tag of the image. Defaults to 'latest'")
    private String tag = "latest";


    @Override
    public void run() {
        if (!validateInput()) {
            return;
        }

        String fullImageName = Constants.REGISTRY_URL + "/" + imageName + ":" + tag;

        log.info("Building image {}:{}.", imageName, tag);
        String imageID = this.buildImage(type, imageName, tag, fullImageName);
        if (imageID == null)
            return;

        this.pushImage(fullImageName);

        log.info("Successfully build and pushed image {}:{} ({}).", imageName, tag, imageID);
    }


    private boolean validateInput() {
        if (!RE_IMAGE_NAME.matcher(imageName).matches()) {
            log.error("Invalid image name. It must match this regex: {}", RE_IMAGE_NAME.pattern());
            return false;
        }
        if (!RE_IMAGE_TAG.matcher(tag).matches()) {
            log.error("Invalid image tag. It must match this regex: {}", RE_IMAGE_TAG.pattern());
            return false;
        }
        if (imageName.startsWith(Constants.REGISTRY_URL) || imageName.contains("/")) {
            log.error("Invalid image name. It can't start with '{}' or contain '/'.", Constants.REGISTRY_URL);
            return false;
        }

        return true;
    }

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
        if (!checkMinecraftPlugins(sourceFolder, type)) {
            log.error("Image {} has no or an invalid DockerMC plugin.", name);
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
                .exec(new ImageBuildCallback());

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

    @SuppressWarnings("unchecked")
    private boolean checkMinecraftPlugins(File sourceFolder, ImageType type) {
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
                    String imageType = (String) content.getOrDefault("type", "");
                    if (imageType.equals(type.getParent().name()))
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

    private boolean pushImage(String imageName) {
        try {
            docker.getDocker().pushImageCmd(imageName)
                    .withAuthConfig(new AuthConfig()
                            .withUsername(mainConfig.get().getRegistryUsername())
                            .withPassword(mainConfig.get().getRegistryPassword()))
                    .exec(new ImagePushCallback())
                    .awaitCompletion()
                    .awaitSuccess();

        } catch (InterruptedException e) {
            log.error("Image push interrupted.");
            return false;
        }

        return true;
    }

    public enum ImageType {
        MINECRAFT,
        BUNGEECORD,
        MC(MINECRAFT),
        BUNGEE(BUNGEECORD),
        BC(BUNGEECORD);

        private ImageType parent = null;

        ImageType() {

        }

        ImageType(ImageType parent) {
            this.parent = parent;
        }

        // Returns the full name of the image type, not just a short version
        public ImageType getParent() {
            if (this.parent == null) {
                return this;
            }

            return this.parent;
        }

    }
}
