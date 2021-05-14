package de.derteufelqwe.ServerManager.setup.templates;

import com.github.dockerjava.api.command.CreateServiceResponse;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.exceptions.InvalidConfigException;
import de.derteufelqwe.ServerManager.utils.Utils;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.annotations.Exclude;
import lombok.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Template to create docker services
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, exclude = {"authConfig"})
public class ServiceTemplate extends DockerObjTemplate {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Exclude
    protected AuthConfig authConfig;

    /**
     * Amount of replicas
     */
    protected int replications;
    /**
     * Constraints where to place the servers. Can be null if it doesn't matter.
     */
    @Nullable
    protected ServiceConstraints constraints;


    public ServiceTemplate(String name, String image, String ramLimit, float cpuLimit, int replications, ServiceConstraints constraints) {
        super(name, image, ramLimit, cpuLimit);
        this.replications = replications;
        this.constraints = constraints;
    }


    @Override
    public FindResponse find() {
        List<Service> services = this.docker.getDocker().listServicesCmd()
                .withNameFilter(Collections.singletonList(this.getName()))
                .exec();

        if (services.size() > 1) {
            throw new FatalDockerMCError("Found multiple services %s named %s.",
                    services.stream().map(Service::getId).collect(Collectors.toList()), this.name);

        } else if (services.size() == 1) {
            return new DockerObjTemplate.FindResponse(true, services.get(0).getId());

        } else {
            return new DockerObjTemplate.FindResponse(false, null);
        }
    }

    @Override
    public CreateResponse create() {
        ServiceSpec serviceSpec = this.getServiceSpec();

        CreateServiceResponse serviceResponse = docker.getDocker().createServiceCmd(serviceSpec)
                .withAuthConfig(this.authConfig)
                .exec();

        this.waitForProcessing();

        return new DockerObjTemplate.CreateResponse(true, serviceResponse.getId());
    }

    @Override
    public DestroyResponse destroy() {
        FindResponse findResponse = this.find();

        if (findResponse.isFound()) {
            this.docker.getDocker().removeServiceCmd(findResponse.getServiceID()).exec();
            return new DockerObjTemplate.DestroyResponse(true, findResponse.getServiceID());
        }

        this.waitForProcessing();

        return new DockerObjTemplate.DestroyResponse(false, null);
    }

    @Override
    public void valid() throws InvalidConfigException {
        super.valid();

        // Constraints
        if (this.constraints != null) {

            if (this.constraints.getNodeLimit() < 0) {
                throw new InvalidConfigException("Service constraints node limit can't be negative.");
            }

        }

        // Replications
        if (this.replications < 0) {
            throw new InvalidConfigException("Replications can't be negative.");
        }

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

        // Authconfig must be set here so the deserialized class has this information too
        MainConfig mainConfig = ServerManager.mainConfig.get();
        this.authConfig = new AuthConfig()
                .withUsername(mainConfig.getRegistryUsername())
                .withPassword(mainConfig.getRegistryPassword());
    }

    /**
     * Waits x seconds for docker to do its magic
     */
    private void waitForProcessing() {
        try {
            TimeUnit.MILLISECONDS.sleep(500);

        } catch (InterruptedException ignored) { }
    }

    // -----  Creation methods  -----

    /**
     * Returns the required network configs.
     *
     * @return
     */
    protected List<NetworkAttachmentConfig> getNetworks() {
        List<NetworkAttachmentConfig> networks = new ArrayList<>();

        networks.add(new NetworkAttachmentConfig()
                .withTarget(Constants.NETW_OVERNET_NAME));

        return networks;
    }

    /**
     * Returns the required labels for the containers of the service.
     *
     * @return
     */
    protected Map<String, String> getContainerLabels() {
        Map<String, String> labels = new HashMap<>();

        labels.put("SERVICE_ID", "{{ .Service.ID }}");
        labels.put("NODE_ID", "{{ .Node.ID }}");
        labels.put("TASK_NAME", "{{ .Task.Name }}");

        return labels;
    }

    /**
     * Returns a list of environment variables for the container.
     *
     * @return
     */
    protected List<String> getEnvs() {
        List<String> envs = new ArrayList<>();

        envs.add("SERVICE_ID={{ .Service.ID }}");
        envs.add("NODE_ID={{ .Node.ID }}");
        envs.add("TASK_NAME={{ .Task.Name }}");

        return envs;
    }

    /**
     * Returns a list of command, which get executed on the container
     */
    protected List<String> getCommandArgs() {
        return new ArrayList<>();
    }

    /**
     * The name of the docker image to use
     */
    protected String getImageName() {
        return "registry.swarm/" + this.image;
    }

    /**
     * A list of mounted volumes
     */
    protected List<Mount> getMountVolumes() {
        return new ArrayList<>();
    }

    /**
     * Returns the ContainerSpec, which describes what a container of a task looks like
     *
     * @return
     */
    protected ContainerSpec getContainerSpec() {
        ContainerSpec containerSpec = new ContainerSpec()
                .withLabels(this.getContainerLabels())
                .withMounts(this.getMountVolumes())
                .withImage(this.getImageName())
                .withEnv(this.getEnvs())
                .withHosts(this.getAdditionalHosts())
                .withArgs(this.getCommandArgs());

        return containerSpec;
    }

    /**
     * Returns the ResourceSpecs, which describe how many resources one task of the service can consume
     *
     * @return
     */
    protected ResourceSpecs getResourceSpecs() {
        long nanoCpu = (long) (this.cpuLimit * 1000000000);
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
                .withPlacement(this.getServicePlacement())
                .withLogDriver(this.getLogDriver());

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
     * If this method returns true, the service will be created in global mode.
     */
    protected boolean useGlobalMode() {
        return false;
    }

    /**
     * Returns the ServiceModeConfig, which specifies the amount of replications of the tasks
     * or the global mode if specified.
     *
     * @return
     */
    protected ServiceModeConfig getServiceModeConfig() {
        ServiceModeConfig serviceModeConfig = new ServiceModeConfig();
        if (this.useGlobalMode()) {
            serviceModeConfig
                    .withGlobal(new ServiceGlobalModeOptions());
        } else {
            serviceModeConfig
                    .withReplicated(new ServiceReplicatedModeOptions().withReplicas(this.replications));
        }

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

    /**
     * Name of the service in docker
     */
    protected String getServiceName() {
        return this.name;
    }

    /**
     * Returns the ServiceSpec, which combines all previously generated classes to fully describe how the
     * service looks like.
     *
     * @return
     */
    public ServiceSpec getServiceSpec() {
        ServiceSpec serviceSpec = new ServiceSpec()
                .withName(this.getServiceName())
                .withLabels(this.getServiceLabels())
                .withTaskTemplate(this.getTaskSpec())
                .withNetworks(this.getNetworks())
                .withMode(this.getServiceModeConfig())
                .withEndpointSpec(this.getEndpointSpec());

        return serviceSpec;
    }

    /**
     * Returns the log driver configuration
     */
    protected Driver getLogDriver() {
        return null;
    }

    /**
     * Returns a list of "ip hostname [aliases...]" which get added to the container /etc/hosts file as additional
     * DNS entries.
     * @return
     */
    private List<String> getAdditionalHosts() {
        List<String> hosts = new ArrayList<>();

        // The real IP of the master node of DockerMC, which is used to access the DB
        hosts.add(String.format("%s %s", ServerManager.mainConfig.get().getDockerMasterIP(), Constants.DMC_MASTER_DNS_NAME));

        return hosts;
    }

    /**
     * Custom implementation to handle constraints copying
     */
    @Override
    public ServiceTemplate clone() {
        ServiceTemplate serviceTemplate = (ServiceTemplate) super.clone();

        if (this.constraints != null) {
            serviceTemplate.constraints = this.constraints.clone();
        }

        return serviceTemplate;
    }

}
