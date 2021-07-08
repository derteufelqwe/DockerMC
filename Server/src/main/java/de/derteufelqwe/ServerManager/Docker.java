package de.derteufelqwe.ServerManager;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import de.derteufelqwe.ServerManager.callbacks.ImagePullCallback;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.exceptions.InvalidServiceConfig;
import de.derteufelqwe.ServerManager.exceptions.TimeoutException;
import de.derteufelqwe.commons.Constants;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Log4j2
public class Docker {

    private Pattern STD_REMOVE = Pattern.compile("^STDOUT: |STDERR: ");

    private MainConfig mainConfig;

    @Getter
    private DockerClient docker;



    public Docker(String protocol, String host, int port, MainConfig config) {
        this.mainConfig = config;
        DockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(String.format("%s://%s:%s", protocol, host, port))
                .withDockerTlsVerify(mainConfig.isUseTLSVerify())
                .withApiVersion(mainConfig.getAPIVersion())
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .build();

        docker = DockerClientImpl.getInstance(clientConfig, httpClient);
    }

    public Docker(MainConfig mainConfig) {
        this(mainConfig.getDockerProtocol(), mainConfig.getDockerIP(), mainConfig.getDockerPort(), mainConfig);
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
    public String getContainerLog(String containerID) throws NotFoundException {
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

            loggingCallback.awaitCompletion(Constants.LOG_FETCH_TIME, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            throw new TimeoutException("Timed out while fetching logs from container %s.", containerID);
        }

        return String.join("\n", logList);
    }

    /**
     * Reads the log from a service and returns them as a string
     *
     * @param serviceID
     * @return
     */
    public String getServiceLog(String serviceID) throws NotFoundException, TimeoutException {
        List<String> logList = new ArrayList<>();
        LogContainerResultCallback loggingCallback = new LogContainerResultCallback() {
            @Override
            public void onNext(Frame item) {
                String line = STD_REMOVE.matcher(item.toString()).replaceAll("");
                logList.add(line);
            }
        };

        try {
            docker.logServiceCmd(serviceID)
                    .withStdout(true)
                    .withStderr(true)
                    .withFollow(true)
                    .withTimestamps(true)
                    .exec(loggingCallback);

            loggingCallback.awaitCompletion(Constants.LOG_FETCH_TIME, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            throw new TimeoutException("Timed out while fetching logs from service %s.", serviceID);
        }

        return String.join("\n", logList);
    }

    /**
     * Pulls an image from the registry
     *
     * @param image Image to pull
     */
    public ImagePullCallback pullImage(String image) throws NotFoundException {
        AuthConfig authConfig = null;

        if (image.startsWith("registry.swarm/")) {
            authConfig = new AuthConfig()
                    .withUsername(mainConfig.getRegistryUsername())
                    .withPassword(mainConfig.getRegistryPassword());
        }

        ImagePullCallback callback = docker.pullImageCmd(image)
                .withAuthConfig(authConfig)
                .exec(new ImagePullCallback());

        return callback;
    }

    public ImagePullCallback pullImage(Constants.Images image) throws NotFoundException {
        return this.pullImage(image.image());
    }

    /**
     * Waits #{timeout} seconds for the service to spawn all its tasks
     *
     * @param service Service to check for
     * @param timeout Timeout to wait
     */
    @SneakyThrows
    public void waitService(Service service, int timeout) throws TimeoutException {
        long startTime = new Date().getTime();
        long numberOfTasks = 0;
        try {
            numberOfTasks = service.getSpec().getMode().getReplicated().getReplicas();
        } catch (NullPointerException e) {
            throw new InvalidServiceConfig("Service %s has an invalid configuration. It's missing a replication number.", service.getId());
        }

        List<Container> containers = new ArrayList<>();
        while (new Date().getTime() <= startTime + timeout * 1000) {
            containers = this.getDocker().listContainersCmd()
                    .withLabelFilter(Collections.singleton("com.docker.swarm.service.id=" + service.getId()))
                    .exec();

            if (containers.size() == numberOfTasks) {
                return;
            }

            TimeUnit.SECONDS.sleep(1);
        }

        throw new TimeoutException("Waiting for service %s timed out after %s seconds. Found %s / %s containers.",
                service.getId(), timeout, containers.size(), numberOfTasks);
    }

    /**
     * Same as waitService but takes a serviceID as an argument
     *
     * @param serviceID
     * @param timeout
     */
    public void waitService(String serviceID, int timeout) throws TimeoutException {
        Service service = this.getDocker().inspectServiceCmd(serviceID).exec();
        this.waitService(service, timeout);
    }


    public void close() throws IOException {
        docker.close();
    }

}
