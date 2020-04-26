package de.derteufelqwe.ServerManager.setup.servers;

import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.backend.Config;
import de.derteufelqwe.ServerManager.config.backend.Ignore;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.setup.ServiceTemplate;
import de.derteufelqwe.commons.Constants;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class ServerTemplate extends ServiceTemplate {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Ignore
    protected MainConfig mainConfig;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Ignore
    protected AuthConfig authConfig;


    // Name prefix
    protected String name;
    // Amount of replicas
    protected int replications;
    // Constraints where to place the servers. Can be null if it doesn't matter. Structure is the following:
    // named / ids -> list(names / ids)
    protected Map<String, List<String>> constraints = new HashMap<>();


    public ServerTemplate(String image, String ramLimit, String cpuLimit, String name, int replications, Map<String, List<String>> constraints) {
        super(image, ramLimit, cpuLimit);
        this.name = name;
        this.replications = replications;
        this.constraints = constraints;
    }

    public ServerTemplate(Docker docker) {
        super(docker);
    }


    /**
     * Initializes the config to be used in the code. If this is not called before using this config, weired errors will
     * occur.
     *
     * @param docker Docker instance to add
     */
    public void init(Docker docker) {
        super.init(docker);
        this.mainConfig = Config.get(MainConfig.class);
        this.authConfig = new AuthConfig()
                .withUsername(this.mainConfig.getRegistryUsername())
                .withPassword(this.mainConfig.getRegistryPassword());
    }

    /**
     * Validates the config and checks whether the config is valid or not
     *
     * @return Returns a ValidationResponse with more information
     */
    public abstract ValidationResponse valid();


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


    // -----  Utility methods  -----

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

    @Override
    protected List<String> getEnvs() {
        List<String> envs = super.getEnvs();

        envs.add("TASK_NAME={{ .Task.Name }}");
        envs.add("SERVER_NAME=" + this.name);

        return envs;
    }
}
