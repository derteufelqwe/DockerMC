package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.commons.Constants;
import lombok.*;

import java.util.*;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class ServiceTemplate extends DockerObjTemplate {

    // Name prefix
    protected String name;
    // Amount of replicas
    protected int replications;
    // Constraints where to place the servers. Can be null if it doesn't matter. Structure is the following:
    protected ServiceConstraints constraints = new ServiceConstraints();


    public ServiceTemplate(String image, String ramLimit, String cpuLimit, String name, int replications, ServiceConstraints constraints) {
        super(image, ramLimit, cpuLimit);
        this.name = name;
        this.replications = replications;
        if (constraints != null) {
            this.constraints = constraints;
        }
    }

    public ServiceTemplate(Docker docker) {
        super(docker);
    }

    // -----  Service creation methods  -----

    /**
     * Returns the required network configs.
     *
     * @return
     */
    protected List<NetworkAttachmentConfig> getNetworks() {
        List<NetworkAttachmentConfig> networks = new ArrayList<>();
        networks.add(new NetworkAttachmentConfig().withTarget(Constants.NETW_OVERNET_NAME));

        return networks;
    }

    /**
     * Returns the required labels for the containers of the service.
     *
     * @return
     */
    protected abstract Map<String, String> getContainerLabels();

    /**
     * Returns a list of environment variables for the container.
     * @return
     */
    protected List<String> getEnvs() {
        List<String> envs = new ArrayList<>();

        return envs;
    }

    /**
     * Returns a list of command, which get executed on the container
     */
    protected List<String> getCommandArgs() {
        return new ArrayList<>();
    }

    protected String getImageName() {
        return "registry.swarm/" + this.image;
    }

    /**
     * Returns the ContainerSpec, which describes what a container of a task looks like
     *
     * @return
     */
    protected ContainerSpec getContainerSpec() {
        ContainerSpec containerSpec = new ContainerSpec()
                .withLabels(this.getContainerLabels())
                .withImage(this.getImageName())
                .withEnv(this.getEnvs())
                .withArgs(this.getCommandArgs());

        return containerSpec;
    }

    /**
     * Returns the ResourceSpecs, which describe how many resources one task of the service can consume
     *
     * @return
     */
    protected ResourceSpecs getResourceSpecs() {
        long nanoCpu = (long) (Double.parseDouble(this.cpuLimit) * 1000000000);
        ResourceSpecs resourceSpecs = new ResourceSpecs()
                .withMemoryBytes(Utils.convertMemoryString(this.ramLimit))
                .withNanoCPUs(nanoCpu);

        return resourceSpecs;
    }


    /**
     * Returns the ServicePlacement, which sets the docker constraints
     *
     * @return
     */
    protected ServicePlacement getServicePlacement() {
        ServicePlacement servicePlacement = new ServicePlacement()
                .withConstraints(this.constraints.getDockerConstraints())
                .withMaxReplicas(this.constraints.getNodeLimit());

        return servicePlacement;
    }

    /**
     * Returns the TaskSpec, which describes how a task looks like
     *
     * @return
     */
    protected TaskSpec getTaskSpec() {
        TaskSpec taskSpec = new TaskSpec()
                .withContainerSpec(this.getContainerSpec())
                .withResources(new ResourceRequirements().withLimits(this.getResourceSpecs()))
                .withPlacement(this.getServicePlacement());

        return taskSpec;
    }

    /**
     * Returns a map with the labels for the service
     *
     * @return
     */
    protected abstract Map<String, String> getServiceLabels();

    /**
     * Returns the ServiceModeConfig, which specifies the amount of replications of the tasks
     *
     * @return
     */
    protected ServiceModeConfig getServiceModeConfig() {
        ServiceModeConfig serviceModeConfig = new ServiceModeConfig()
                .withReplicated(new ServiceReplicatedModeOptions().withReplicas(this.replications));

        return serviceModeConfig;
    }

    /**
     * Returns a list of published ports for the service
     */
    protected List<PortConfig> getPortList() {
        return new ArrayList<>();
    }

    /**
     * Returns the EndpointSpec, which describes the published ports
     */
    protected EndpointSpec getEndpointSpec() {
        EndpointSpec endpointSpec = new EndpointSpec()
                .withPorts(this.getPortList());

        return endpointSpec;
    }

    protected String getServiceName() {
        return null;
    }

    /**
     * Returns the ServiceSpec, which combines all previously generated classes to fully describe how the
     * service looks like.
     *
     * @return
     */
    protected ServiceSpec getServiceSpec() {
        ServiceSpec serviceSpec = new ServiceSpec()
                .withLabels(this.getServiceLabels())
                .withTaskTemplate(this.getTaskSpec())
                .withNetworks(this.getNetworks())
                .withMode(this.getServiceModeConfig())
                .withEndpointSpec(this.getEndpointSpec())
                .withName(this.getServiceName())
        ;

        return serviceSpec;
    }

}
