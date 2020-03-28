package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.Config;
import de.derteufelqwe.ServerManager.config.configs.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.configs.MainConfig;
import de.derteufelqwe.ServerManager.config.configs.objects.BungeeProxy;
import de.derteufelqwe.ServerManager.setup.objects.BungeeCreateResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Creates Bungeecord instances from the config
 */
public class BungeeProxyCreator extends CreatorBase {


    public BungeeProxyCreator() {
        super();
    }


    public void create() {
        BungeeCreateResponse r = createContainer(this.config.getProxy());
        if (!r.successful()) {
            System.err.println("Failed to create proxy");
            System.out.println(r.getLogs());
        }

    }


    private BungeeCreateResponse createContainer(BungeeProxy proxyConfig) {
        String imageName = "registry.swarm/" + proxyConfig.getImage();
        this.pullImage(imageName);

        CreateContainerResponse createResponse = docker.getDocker().createContainerCmd(imageName)
                .withLabels(Utils.quickLabel(Constants.ContainerType.BUNGEE))
                .withAuthConfig(this.authConfig)
                .withPortBindings(new PortBinding(Ports.Binding.bindPort(proxyConfig.getPort()), ExposedPort.tcp(25577)))
                .exec();

        String containerID = createResponse.getId();

        docker.getDocker().connectToNetworkCmd()
                .withContainerId(containerID)
                .withNetworkId(Constants.NETW_OVERNET_NAME)
                .exec();

        docker.getDocker().connectToNetworkCmd()
                .withContainerId(containerID)
                .withNetworkId(Constants.NETW_API_NAME)
                .exec();

        docker.getDocker().startContainerCmd(containerID)
                .exec();

        try {
            docker.getDocker().waitContainerCmd(containerID)
                    .exec(new WaitContainerResultCallback())
                    .awaitStarted(CONTAINER_START_DELAY, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }

        return new BungeeCreateResponse(containerID);
    }


}
