package de.derteufelqwe.ServerManager;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import de.derteufelqwe.ServerManager.config.Config;
import de.derteufelqwe.ServerManager.config.configs.MainConfig;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Docker {

    private Pattern STD_REMOVE = Pattern.compile("^STDOUT: |STDERR: ");
    private int PULL_INTERVAL = 5;  // Pause between Pull checks
    private int PULL_REPETITIONS = 25;  // Amount of times the interval gets waited

    @Getter
    private DockerClient docker;

    public Docker() {
        MainConfig mainConfig = Config.get(MainConfig.class);
        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(String.format("%s://%s:%s", mainConfig.getDockerProtocol(), mainConfig.getDockerIP(), mainConfig.getDockerPort()))
                .withDockerTlsVerify(mainConfig.isUseTLSVerify())
                .withApiVersion(mainConfig.getAPIVersion())
                .build();

        docker = DockerClientImpl.getInstance(dockerClientConfig)
                .withDockerCmdExecFactory(new NettyDockerCmdExecFactory());


    }


    /**
     * Short version for the docker exec command.
     * This sends a command to a docker container.
     *
     * @param containerID Container to send to
     * @param command     Command to execute
     * @return Success or failure
     */
    public boolean execContainer(String containerID, String... command) {
        ByteArrayOutputStream containerLog = new ByteArrayOutputStream();

        String execID = docker.execCreateCmd(containerID)
                .withAttachStdout(true)
                .withCmd(command)
                .exec().getId();

        try {
            docker.execStartCmd(execID)
                    .exec(new ExecStartResultCallback(containerLog, null))
                    .awaitCompletion(10, TimeUnit.SECONDS);

            int exitCode = docker.inspectExecCmd(execID).exec().getExitCode();

            if (exitCode != 0) {
                System.err.println(String.format("Failed to execute command \"%s\" in container %s with code %s. Log:",
                        String.join(" ", command), containerID, exitCode));
                System.out.println(containerLog);
                return false;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Reads the log from a container and returns them as a string
     *
     * @param containerID
     * @return
     */
    public String getContainerLog(String containerID) {
        List<String> logList = new ArrayList<>();
        LogContainerResultCallback loggingCallback = new LogContainerResultCallback() {
            @Override
            public void onNext(Frame item) {
                String line = STD_REMOVE.matcher(item.toString()).replaceAll("");
                logList.add(line);
            }
        };

        try {
            docker.logContainerCmd(containerID)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(loggingCallback);

            loggingCallback.awaitCompletion(10, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            return "";

        } catch (NotFoundException e2) {    // Container doesn't exist
            return null;
        }

        return String.join("\n", logList);
    }


    public void pullImage(String image) throws FatalDockerMCError {
        System.out.println("Pulling image " + image + "...");
        try {
            MainConfig mainConfig = Config.get(MainConfig.class);
            AuthConfig authConfig = new AuthConfig()
                    .withUsername(mainConfig.getRegistryUsername())
                    .withPassword(mainConfig.getRegistryPassword());

            docker.pullImageCmd(image)
                    .withAuthConfig(authConfig)
                    .exec(new PullImageResultCallback() {
                        @Override
                        public void onNext(PullResponseItem item) {
                            super.onNext(item);
//                            System.out.println(item.toString());
                        }
                    })
                    .awaitCompletion(120, TimeUnit.SECONDS);


            TimeUnit.SECONDS.sleep(PULL_INTERVAL);
            int imageCount = docker.listImagesCmd()
                    .withShowAll(true)
                    .withImageNameFilter(image)
                    .exec().size();

            if (imageCount == 0) {
                throw new FatalDockerMCError("Image " + image + " not found. This may be caused by a slow image download " +
                        "(download taking longer than 120 seconds). Try downloading the image by hand.");
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
