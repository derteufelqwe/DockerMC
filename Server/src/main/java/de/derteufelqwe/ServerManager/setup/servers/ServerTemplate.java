package de.derteufelqwe.ServerManager.setup.servers;

import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.backend.Config;
import de.derteufelqwe.ServerManager.config.backend.Ignore;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.setup.ServiceConstraints;
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


    public ServerTemplate(String image, String ramLimit, String cpuLimit, String name, int replications, ServiceConstraints constraints) {
        super(image, ramLimit, cpuLimit, name, replications, constraints);
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


    @Override
    protected List<String> getEnvs() {
        List<String> envs = super.getEnvs();

        envs.add("TASK_NAME={{ .Task.Name }}");
        envs.add("SERVER_NAME=" + this.name);

        return envs;
    }
}
