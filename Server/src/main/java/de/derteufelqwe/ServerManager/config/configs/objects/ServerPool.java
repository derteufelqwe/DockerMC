package de.derteufelqwe.ServerManager.config.configs.objects;

import com.github.dockerjava.api.command.CreateServiceResponse;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.commons.Constants;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ServerPool extends ServerBase {

    // Soft playerlimit
    private int softPlayerLimit;


    @Override
    public FindResponse find() {

        List<Service> services = this.docker.getDocker().listServicesCmd()
                .withLabelFilter(this.getServiceLabels())
                .exec();

        if (services.size() > 1) {
            throw new FatalDockerMCError("Found multiple services %s for the config %s.",
                    services.stream().map(Service::getId).collect(Collectors.joining(", ")), this.name);

        } else if (services.size() == 1) {
            return new ServerBase.FindResponse(true, services.get(0).getId());

        } else {
            return new ServerBase.FindResponse(false, null);
        }
    }

    @Override
    public CreateResponse create() {
        ServiceSpec serviceSpec = this.getServiceSpec();

        CreateServiceResponse serviceResponse = docker.getDocker().createServiceCmd(serviceSpec)
                .withAuthConfig(this.authConfig)
                .exec();

        return new ServerBase.CreateResponse(true, serviceResponse.getId());
    }

    @Override
    public DestroyResponse destroy() {
        FindResponse findResponse = this.find();

        if (findResponse.isFound()) {
            this.docker.getDocker().removeServiceCmd(findResponse.getServiceID()).exec();
            return new ServerBase.DestroyResponse(true, findResponse.getServiceID());
        }

        return new ServerBase.DestroyResponse(false, null);
    }

    // -----  Utility methods  -----

    private List<NetworkAttachmentConfig> getNetworks() {
        List<NetworkAttachmentConfig> networks = new ArrayList<>();
        networks.add(new NetworkAttachmentConfig().withTarget(Constants.NETW_OVERNET_NAME));

        return networks;
    }

    private Map<String, String> getContainerLabels() {
        Map<String, String> containerLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT);
        containerLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return containerLabels;
    }

    private ContainerSpec getContainerSpec() {
        ContainerSpec containerSpec = new ContainerSpec()
                .withLabels(this.getContainerLabels())
                .withImage("registry.swarm/" + this.image);

        return containerSpec;
    }

    private ResourceSpecs getResourceSpecs() {
        long nanoCpu = (long) (Double.parseDouble(this.cpuLimit) * 1000000000);
        ResourceSpecs resourceSpecs = new ResourceSpecs()
                .withMemoryBytes(Utils.convertMemoryString(this.ramLimit))
                .withNanoCPUs(nanoCpu);

        return resourceSpecs;
    }

    private TaskSpec getTaskSpec() {
        TaskSpec taskSpec = new TaskSpec()
                .withContainerSpec(this.getContainerSpec())
                .withResources(new ResourceRequirements().withLimits(this.getResourceSpecs()));

        return taskSpec;
    }

    private Map<String, String> getServiceLabels() {
        Map<String, String> serviceLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL);
        serviceLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return serviceLabels;
    }

    private ServiceModeConfig getServiceModeConfig() {
        ServiceModeConfig serviceModeConfig = new ServiceModeConfig().withReplicated(
                new ServiceReplicatedModeOptions().withReplicas(this.replications));

        return serviceModeConfig;
    }

    private ServiceSpec getServiceSpec() {
        ServiceSpec serviceSpec = new ServiceSpec()
                .withLabels(this.getServiceLabels())
                .withTaskTemplate(this.getTaskSpec())
                .withNetworks(this.getNetworks())
                .withMode(this.getServiceModeConfig());

        return serviceSpec;
    }

}
