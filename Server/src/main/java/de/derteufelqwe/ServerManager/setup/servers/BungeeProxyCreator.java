package de.derteufelqwe.ServerManager.setup.servers;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import de.derteufelqwe.ServerManager.setup.servers.responses.Response;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.configs.objects.BungeeProxy;
import de.derteufelqwe.ServerManager.setup.servers.responses.BungeeResponse;

import java.util.concurrent.TimeUnit;

/**
 * Creates Bungeecord instances from the config
 */
public class BungeeProxyCreator extends CreatorBase {


    public BungeeProxyCreator() {
        super();
    }


    public Response create() {
        BungeeResponse r = createContainer(this.config.getProxy());
        if (!r.successful()) {
            System.err.println("Failed to create proxy");
            System.out.println(r.getLogs());
        }

        return null;
    }


    private BungeeResponse createContainer(BungeeProxy proxyConfig) {
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

        return new BungeeResponse(containerID);
    }


}
