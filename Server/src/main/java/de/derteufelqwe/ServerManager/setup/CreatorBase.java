package de.derteufelqwe.ServerManager.setup;

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

}
