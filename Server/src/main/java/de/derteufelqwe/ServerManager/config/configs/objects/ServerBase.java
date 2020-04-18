package de.derteufelqwe.ServerManager.config.configs.objects;

import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.Config;
import de.derteufelqwe.ServerManager.config.Ignore;
import de.derteufelqwe.ServerManager.config.configs.MainConfig;
import de.derteufelqwe.commons.Constants;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class ServerBase {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Ignore
    protected MainConfig mainConfig;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Ignore
    protected Docker docker;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Ignore
    protected AuthConfig authConfig;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Ignore
    private boolean isInit = false;

    // Name prefix
    protected String name;
    // Image
    protected String image;
    // Amount of replicas
    protected int replications;
    // RAM limit per task, like 2G or 512M
    protected String ramLimit;
    // CPU limit per task like 1
    protected String cpuLimit;
    // Constraints where to place the servers. Can be null if it doesn't matter. Structure is the following:
    // named / ids -> list(names / ids)
    protected Map<String, List<String>> constraints = new HashMap<>();


    /**
     * Initializes the config to be used in the code. If this is not called before using this config, weired errors will
     * occur.
     *
     * @param docker Docker instance to add
     */
    public void init(Docker docker) {
        if (!isInit) {
            this.docker = docker;
            this.mainConfig = Config.get(MainConfig.class);
            this.authConfig = new AuthConfig()
                    .withUsername(this.mainConfig.getRegistryUsername())
                    .withPassword(this.mainConfig.getRegistryPassword());

            this.isInit = true;
        }
    }

    /**
     * Validates the config and checks whether the config is valid or not
     *
     * @return Returns a ValidationResponse with more information
     */
    public abstract ValidationResponse valid();

    /**
     * Tries to find an existing instance of this config.
     *
     * @return Returns a FindResponse with all necessary data.
     */
    public abstract FindResponse find();

    /**
     * Creates the instance of this config.
     *
     * @return Returns a CreateResponse with all necessary data.
     */
    public abstract CreateResponse create();

    /**
     * Tries to destroy an existing instance of this config.
     *
     * @return Returns a DestroyResponse with all necessary data.
     */
    public abstract DestroyResponse destroy();

    // -----  Other methods  -----

    /**
     * Basic validation if parameters are not null.
     * @return List with all parameter names that are null.
     */
    protected List<String> validateParamsNotNull() {
        List<String> resultList = new ArrayList<>();

        if (this.name == null) {
            resultList.add("name");
        }
        if (this.image == null) {
            resultList.add("image");
        }
        if (this.replications == 0) {
            resultList.add("replications");
        }
        if (this.ramLimit == null) {
            resultList.add("ramLimit");
        }
        if (this.cpuLimit == null) {
            resultList.add("cpuLimit");
        }

        return resultList;
    }


    // -----  Responses  -----

    /**
     * Response to the valid() function.
     */
    @Data
    @AllArgsConstructor
    public class ValidationResponse {

        private boolean valid;
        private String name;
        private String reason;

    }

    /**
     * Response to the find() function.
     */
    @Data
    @AllArgsConstructor
    public class FindResponse {

        private boolean found;
        private String serviceID;

    }

    /**
     * Response to the create() function.
     */
    @Data
    @AllArgsConstructor
    public class CreateResponse {

        private boolean created;
        private String serviceID;

    }

    /**
     * Response to the destroy() function.
     */
    @Data
    @AllArgsConstructor
    public class DestroyResponse {

        private boolean destroyed;
        private String serviceID;

    }

    // -----  Utility methods  -----

    /**
     * Returns the required network configs.
     * @return
     */
    protected List<NetworkAttachmentConfig> getNetworks() {
        List<NetworkAttachmentConfig> networks = new ArrayList<>();
        networks.add(new NetworkAttachmentConfig().withTarget(Constants.NETW_OVERNET_NAME));

        return networks;
    }

    /**
     * Returns the required labels for the containers of the service.
     * @return
     */
    protected abstract Map<String, String> getContainerLabels();

    /**
     * Returns the ContainerSpec, which describes what a container of a task looks like
     * @return
     */
    protected ContainerSpec getContainerSpec() {
        ContainerSpec containerSpec = new ContainerSpec()
                .withLabels(this.getContainerLabels())
                .withImage("registry.swarm/" + this.image);

        return containerSpec;
    }

    /**
     * Returns the ResourceSpecs, which describe how many resources one task of the service can consume
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
     * Parses the constraints data and returns a dict with the parsed constraints
     * @return
     */
    private Map<String, List<String>> parseConstraints() {
        Map<String, List<String>> constraintsMap = new HashMap<>();
        constraintsMap.put("ids", new ArrayList<>());       // Ids
        constraintsMap.put("names", new ArrayList<>());     // Names
        constraintsMap.put("!ids", new ArrayList<>());      // Ids to not run on
        constraintsMap.put("!names", new ArrayList<>());    // Names to not run on

        if (this.constraints == null) {
            return constraintsMap;
        }

        if (this.constraints.containsKey("ids") && this.constraints.get("ids") != null) {
            for (String idConstr : this.constraints.get("ids")) {
                if (idConstr.substring(0, 1).equals("!")) {
                    constraintsMap.get("!ids").add(idConstr.substring(1));

                } else {
                    constraintsMap.get("ids").add(idConstr);
                }
            }
        }

        if (this.constraints.containsKey("names") && this.constraints.get("names") != null) {
            for (String nameConstr : this.constraints.get("names")) {
                if (nameConstr.substring(0, 1).equals("!")) {
                    constraintsMap.get("!names").add(nameConstr.substring(1));

                } else {
                    constraintsMap.get("names").add(nameConstr);
                }
            }
        }

        return constraintsMap;
    }

    /**
     * Returns a list of docker constraints, which limit where tasks can run.
     * ATTENTION: This can result in tasks not beeing able to run anywhere.
     * @return
     */
    protected List<String> getConstraintsList() {
        List<String> constraintsList = new ArrayList<>();
        Map<String, List<String>> constraintsMap = this.parseConstraints();

        for (String constraint : constraintsMap.get("ids")) {
            constraintsList.add("node.id==" + constraint);
        }

        for (String constraint : constraintsMap.get("names")) {
            constraintsList.add("node.labels.name==" + constraint);
        }

        for (String constraint : constraintsMap.get("!ids")) {
            constraintsList.add("node.id!=" + constraint);
        }

        for (String constraint : constraintsMap.get("!names")) {
            constraintsList.add("node.labels.name!=" + constraint);
        }

        return constraintsList;
    }

    /**
     * Returns the ServicePlacement, which sets the docker constraints
     * @return
     */
    protected ServicePlacement getServicePlacement() {
        ServicePlacement servicePlacement = new ServicePlacement()
                .withConstraints(this.getConstraintsList());

        return servicePlacement;
    }

    /**
     * Returns the TaskSpec, which describes how a task looks like
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
     * @return
     */
    protected abstract Map<String, String> getServiceLabels();

    /**
     * Returns the ServiceModeConfig, which specifies the amount of replications of the tasks
     * @return
     */
    protected ServiceModeConfig getServiceModeConfig() {
        ServiceModeConfig serviceModeConfig = new ServiceModeConfig().withReplicated(
                new ServiceReplicatedModeOptions().withReplicas(this.replications));

        return serviceModeConfig;
    }

    /**
     * Returns the ServiceSpec, which combines all previously generated classes to fully describe how the
     * service looks like.
     * @return
     */
    protected ServiceSpec getServiceSpec() {
        ServiceSpec serviceSpec = new ServiceSpec()
                .withLabels(this.getServiceLabels())
                .withTaskTemplate(this.getTaskSpec())
                .withNetworks(this.getNetworks())
                .withMode(this.getServiceModeConfig());

        return serviceSpec;
    }
    
}
