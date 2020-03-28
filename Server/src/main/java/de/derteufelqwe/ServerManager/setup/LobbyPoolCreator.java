package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateServiceResponse;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.configs.objects.LobbyServerPool;
import de.derteufelqwe.commons.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LobbyPoolCreator extends CreatorBase {

    public LobbyPoolCreator() {
        super();
    }


    public void create() {
        createContainer(this.config.getLobbyPool());
    }

    private void createContainer(LobbyServerPool lobbyConfig) {
        String imageName = "registry.swarm/" + lobbyConfig.getImage();
        this.pullImage(imageName);

        // Overnet network
        List<NetworkAttachmentConfig> networks = new ArrayList<>();
        networks.add(new NetworkAttachmentConfig().withTarget(Constants.NETW_OVERNET_NAME));

        // Normal labels + name of the server
        Map<String, String> containerLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT);
        containerLabels.put(Constants.SERVER_NAME_KEY, lobbyConfig.getName());

        ContainerSpec containerSpec = new ContainerSpec()
                .withLabels(containerLabels)
                .withImage(imageName);

        TaskSpec taskSpec = new TaskSpec()
                .withContainerSpec(containerSpec);

        // Normal labels + name of the server
        Map<String, String> serviceLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL);
        serviceLabels.put(Constants.SERVER_NAME_KEY, lobbyConfig.getName());

        // Specify the number of replications
        ServiceModeConfig serviceModeConfig = new ServiceModeConfig().withReplicated(
                new ServiceReplicatedModeOptions().withReplicas(lobbyConfig.getReplications()));

        ServiceSpec serviceSpec = new ServiceSpec()
                .withLabels(serviceLabels)
                .withTaskTemplate(taskSpec)
                .withNetworks(networks)
                .withMode(serviceModeConfig);

        CreateServiceResponse serviceResponse = docker.getDocker().createServiceCmd(serviceSpec)
                .withAuthConfig(this.authConfig)
                .exec();

    }

}
