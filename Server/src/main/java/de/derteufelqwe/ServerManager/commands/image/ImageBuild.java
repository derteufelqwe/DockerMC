package de.derteufelqwe.ServerManager.commands.image;

import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.command.PushImageResultCallback;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.commons.Constants;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Builds an image to use in the config
 */
@CommandLine.Command(name = "build", description = "Builds an image to use on the network.",
        mixinStandardHelpOptions = true, subcommands = {

})
public class ImageBuild implements Runnable {

    private final Pattern REGISTRY_TAG = Pattern.compile("^registry\\.swarm\\/");

    private Docker docker;

    @CommandLine.Parameters(index = "0", description = "Location of the imagefolder like minecraft/default")
    private String imageLocation;

    @CommandLine.Parameters(index = "1", description = "The name of the image WITHOUT registry.swarm tag.")
    private String imageName;

    @CommandLine.Parameters(index = "2", description = "Type of the image. Wrong choice will result in build error.")
    private ImageType imageType;    // ToDo: Autocompletion hinzufügen

    @CommandLine.Option(names = {"-u", "--update"}, description = "Update an image. Prevents accidental overwriting of images")
    private boolean update = false; // ToDo: Not implemented

    @CommandLine.Option(names = {"--no-push"}, description = "Don't push image to registry. This can cause problems!")
    private boolean noPush = false;

    @CommandLine.Option(names = {"--timeout"}, description = "Timeout in seconds, when the build will be canceled.")
    private int buildTimeout = 120;


    public ImageBuild() {
        this.docker = ServerManager.getDocker();
    }


    @Override
    public void run() {
        System.out.println(String.format("Building image %s...", this.imageName));

        File dockerfileSrc = new File(Constants.DOCKERFILES_PATH + this.imageType.fileName());
        File dockerfileDst = new File(Constants.IMAGE_PATH + this.imageLocation + "/Dockerfile");

        // --- Setup checks ---
        // Check if Dockerfile exists
        if (!dockerfileSrc.exists()) {
            System.err.println(String.format("Couldn't find Dockerfile %s.", this.imageType.fileName()));
            return;
        }
        // Check if Image-Folder exists
        if (!new File(Constants.IMAGE_PATH + this.imageLocation).exists()) {
            System.err.println(String.format("Couldn't find image folder %s.", this.imageLocation));
            return;
        }
        // Check image name
        if (REGISTRY_TAG.matcher(this.imageName).find()) {
            System.err.println("Image name shouldn't start with registry.swarm!");
            return;
        }

        // --- Build image ---

        try {
            FileUtils.copyFile(dockerfileSrc, dockerfileDst);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String imageID = docker.getDocker().buildImageCmd()
                .withDockerfile(dockerfileDst)
                .withTags(Collections.singleton("registry.swarm/" + this.imageName))
                .exec(new BuildImageResultCallback())
                .awaitImageId(this.buildTimeout, TimeUnit.SECONDS);

        dockerfileDst.delete();

        if (this.update) {
            System.out.println(String.format("Successfully updated image %s.", this.imageName));
        } else {
            System.out.println(String.format("Successfully built image %s.", this.imageName));
        }


        if (!this.noPush) {
            System.out.println("Pushing image to registry...");
            MainConfig mainConfig = ServerManager.CONFIG.get(MainConfig.class);
            String username = mainConfig.getRegistryUsername();
            String password = mainConfig.getRegistryPassword();

            try {
                docker.getDocker().pushImageCmd("registry.swarm/" + this.imageName)
                        .withAuthConfig(new AuthConfig().withUsername(username).withPassword(password))
                        .exec(new PushImageResultCallback())
                        .awaitCompletion(this.buildTimeout, TimeUnit.SECONDS);

            } catch (InterruptedException e) {
                System.err.println("Pushing image interrupted!");
                return;
            }

            System.out.println(String.format("Successfully pushed image %s.", this.imageName));
        }

    }

    enum ImageType {
        BUNGEE("Waterfall.dfile"),
        MINECRAFT("Minecraft.dfile");

        private String dockerFileName;

        ImageType(String name) {
            this.dockerFileName = name;
        }

        public String fileName() {
            return this.dockerFileName;
        }

    }

}
