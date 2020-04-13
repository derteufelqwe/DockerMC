package de.derteufelqwe.ServerManager.setup.servers;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.servers.responses.Response;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.configs.objects.BungeeProxy;
import de.derteufelqwe.ServerManager.setup.servers.responses.BungeeResponse;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Creates Bungeecord instances from the config
 */
public class BungeeProxyCreator extends CreatorBase {


    public BungeeProxyCreator() {
        super();
    }


    public Response create(BungeeProxy proxyConfig) {
        return this.createService(proxyConfig);
    }


    private BungeeResponse createService(BungeeProxy proxyConfig) {
        String imageName = "registry.swarm/" + proxyConfig.getImage();
        this.pullImage(imageName);

        Bind bind = new Bind(Constants.API_CERTS_PATH + "client/", new Volume("/certs"));

        ExposedPort exposedPort = ExposedPort.tcp(25577);
        Ports ports = new Ports();
        ports.bind(exposedPort, Ports.Binding.bindPort(25578));

        CreateContainerResponse createResponse = docker.getDocker().createContainerCmd(imageName)
                .withLabels(Utils.quickLabel(Constants.ContainerType.BUNGEE))
                .withAuthConfig(this.authConfig)
                .withExposedPorts(exposedPort)
                .withPortBindings(ports)
                .withBinds(bind)
                .exec();

        String containerID = createResponse.getId();

        docker.getDocker().connectToNetworkCmd()
                .withContainerId(containerID)
                .withNetworkId(Constants.NETW_OVERNET_NAME)
                .exec();

//        docker.getDocker().connectToNetworkCmd()
//                .withContainerId(containerID)
//                .withNetworkId(Constants.NETW_API_NAME)
//                .exec();

        docker.getDocker().startContainerCmd(containerID)
                .exec();

        try {
            docker.getDocker().waitContainerCmd(containerID)
                    .exec(new WaitContainerResultCallback())
                    .awaitStarted(CONTAINER_START_DELAY, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }

        return new BungeeResponse(containerID, proxyConfig);
    }


    public String findService(BungeeProxy proxyConfig) {
        List<Service> services = this.docker.getDocker().listServicesCmd()
                .withLabelFilter(Utils.quickLabel(Constants.ContainerType.BUNGEE))
                .exec();

        if (services.size() > 1) {
            throw new FatalDockerMCError("Found multiple services %s for the config %s.",
                    services.stream().map(Service::getId).collect(Collectors.joining(", ")), proxyConfig.getName());

        } else if (services.size() == 1) {
            return services.get(0).getId();

        } else {
            return null;
        }
    }

}
