package de.derteufelqwe.ServerManager.setup.servers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.AuthConfig;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.Config;
import de.derteufelqwe.ServerManager.config.configs.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.configs.MainConfig;

import java.util.ArrayList;
import java.util.List;

public class CreatorBase {

    protected final int CONTAINER_START_DELAY = 10;
    protected AuthConfig authConfig;

    protected Docker docker;
    protected InfrastructureConfig config;
    protected MainConfig mainConfig;

    private List<String> pulledImages = new ArrayList<>();

    public CreatorBase() {
        this.docker = ServerManager.getDocker();
        this.config = Config.get(InfrastructureConfig.class);
        this.mainConfig = Config.get(MainConfig.class);
        this.authConfig = new AuthConfig()
                .withUsername(this.mainConfig.getRegistryUsername())
                .withPassword(this.mainConfig.getRegistryPassword());
    }


    protected void pullImage(String imageName) {
        if (!this.pulledImages.contains(imageName)) {
            docker.pullImage(imageName);
            this.pulledImages.add(imageName);
        }
    }


    public class Response {

        private String containerID;
        private Docker docker;

        public Response(String containerID) {
            this.containerID = containerID;
            this.docker = ServerManager.getDocker();
        }


        public boolean successful() {
            InspectContainerResponse response = this.docker.getDocker().inspectContainerCmd(this.containerID)
                    .exec();

            return response.getState().getRunning() == null ? false : response.getState().getRunning();
        }


        public String getLogs() {
            return this.docker.getContainerLog(this.containerID);
        }

    }

}
