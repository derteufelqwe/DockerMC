package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.command.CreateServiceResponse;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.backend.Config;
import de.derteufelqwe.ServerManager.config.backend.Ignore;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.servers.ServerTemplate;
import de.derteufelqwe.commons.Constants;
import lombok.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Template to create docker services
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ServiceTemplate extends DockerObjTemplate {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Ignore
    protected AuthConfig authConfig;


    // Amount of replicas
    protected int replications;
    // Constraints where to place the servers. Can be null if it doesn't matter.
    @Nullable protected ServiceConstraints constraints;


    public ServiceTemplate(String name, String image, String ramLimit, String cpuLimit, int replications, ServiceConstraints constraints) {
        super(name, image, ramLimit, cpuLimit);
        this.replications = replications;
        this.constraints = constraints;
    }

    public ServiceTemplate(Docker docker) {
        super(docker);
    }


    @Override
    public FindResponse find() {
        List<Service> services = this.docker.getDocker().listServicesCmd()
                .withLabelFilter(this.getServiceLabels())
                .exec();

        if (services.size() > 1) {
            throw new FatalDockerMCError("Found multiple services %s for %s.",
                    services.stream().map(Service::getId).collect(Collectors.joining(", ")), this.name);

        } else if (services.size() == 1) {
            return new ServerTemplate.FindResponse(true, services.get(0).getId());

        } else {
            return new ServerTemplate.FindResponse(false, null);
        }
    }

    @Override
    public CreateResponse create() {
        ServiceSpec serviceSpec = this.getServiceSpec();

        CreateServiceResponse serviceResponse = docker.getDocker().createServiceCmd(serviceSpec)
                .withAuthConfig(this.authConfig)
                .exec();

        this.waitForProcessing();

        return new ServerTemplate.CreateResponse(true, serviceResponse.getId());
    }

    @Override
    public DestroyResponse destroy() {
        FindResponse findResponse = this.find();

        if (findResponse.isFound()) {
            this.docker.getDocker().removeServiceCmd(findResponse.getServiceID()).exec();
            return new ServerTemplate.DestroyResponse(true, findResponse.getServiceID());
        }

        this.waitForProcessing();

        return new ServerTemplate.DestroyResponse(false, null);
    }


    // -----  Other methods  -----

    /**
     * Initializes the config to be used in the code. If this is not called before using this config, weired errors will
     * occur.
     *
     * @param docker Docker instance to add
     */
    public void init(Docker docker) {
        super.init(docker);
        MainConfig mainConfig = Config.get(MainConfig.class);
        this.authConfig = new AuthConfig()
                .withUsername(mainConfig.getRegistryUsername())
                .withPassword(mainConfig.getRegistryPassword());
    }

    /**
     * Basic validation if parameters are not null.
     *
     * @return List with all parameter names that are null.
     */
    protected List<String> findNullParams() {
        List<String> resultList = super.findNullParams();

        if (this.replications == 0) {
            resultList.add("replications");
        }

        return resultList;
    }

    /**
     * Waits x seconds for docker to do its magic
     */
    @SneakyThrows
    private void waitForProcessing() {
        TimeUnit.MILLISECONDS.sleep(500);
    }

    // -----  Creation methods  -----

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
    protected Map<String, String> getContainerLabels() {
        return new HashMap<>();
    }

    /**
     * Returns a list of environment variables for the container.
     *
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
        if (this.constraints == null) {
            return new ServicePlacement();
        }

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
    protected Map<String, String> getServiceLabels() {
        return new HashMap<>();
    }

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
                .withName(this.getServiceName());

        return serviceSpec;
    }

}
