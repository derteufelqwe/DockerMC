package de.derteufelqwe.ServerManager.setup.servers;

import com.github.dockerjava.api.command.CreateServiceResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.configs.objects.ServerPool;
import de.derteufelqwe.commons.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerPoolCreator extends CreatorBase {

    public ServerPoolCreator() {
        super();
    }

    public void create() {
        for (ServerPool pool : this.config.getPoolServers()) {
            System.out.println("Creating Pool " + pool.getName() + ".");
            createContainer(pool);
        }
    }

    private Response createContainer(ServerPool lobbyConfig) {
        String imageName = "registry.swarm/" + lobbyConfig.getImage();
        // this.pullImage(imageName);

        // Overnet network
        List<NetworkAttachmentConfig> networks = new ArrayList<>();
        networks.add(new NetworkAttachmentConfig().withTarget(Constants.NETW_OVERNET_NAME));

        // Normal labels + name of the server (for Task)
        Map<String, String> containerLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT);
        containerLabels.put(Constants.SERVER_NAME_KEY, lobbyConfig.getName());

        // Define the containers
        ContainerSpec containerSpec = new ContainerSpec()
                .withLabels(containerLabels)
                .withImage(imageName);

        // Limit the container usage
        long nanoCpu = (long) (Double.parseDouble(lobbyConfig.getCpuLimit()) * 1000000000);
        ResourceSpecs resourceSpecs = new ResourceSpecs()
                .withMemoryBytes(Utils.convertMemoryString(lobbyConfig.getRamLimit()))
                .withNanoCPUs(nanoCpu);

        // Specify the tasks
        TaskSpec taskSpec = new TaskSpec()
                .withContainerSpec(containerSpec)
                .withResources(new ResourceRequirements().withLimits(resourceSpecs));

        // Normal labels + name of the server (for Service)
        Map<String, String> serviceLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL);
        serviceLabels.put(Constants.SERVER_NAME_KEY, lobbyConfig.getName());

        // Specify the number of replications
        ServiceModeConfig serviceModeConfig = new ServiceModeConfig().withReplicated(
                new ServiceReplicatedModeOptions().withReplicas(lobbyConfig.getReplications()));

        // Create the service
        ServiceSpec serviceSpec = new ServiceSpec()
                .withLabels(serviceLabels)
                .withTaskTemplate(taskSpec)
                .withNetworks(networks)
                .withMode(serviceModeConfig);

        CreateServiceResponse serviceResponse = docker.getDocker().createServiceCmd(serviceSpec)
                .withAuthConfig(this.authConfig)
                .exec();

        return new Response(serviceResponse.getId());
    }


    private boolean checkStatus(Response response) {
        return true;
    }


}
