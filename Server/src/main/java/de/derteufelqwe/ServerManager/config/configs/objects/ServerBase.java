package de.derteufelqwe.ServerManager.config.configs.objects;

import com.github.dockerjava.api.model.AuthConfig;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.config.Config;
import de.derteufelqwe.ServerManager.config.Ignore;
import de.derteufelqwe.ServerManager.config.configs.MainConfig;
import lombok.*;

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


    public void init(Docker docker) {
        this.docker = docker;
        this.mainConfig = Config.get(MainConfig.class);
        this.authConfig = new AuthConfig()
                .withUsername(this.mainConfig.getRegistryUsername())
                .withPassword(this.mainConfig.getRegistryPassword());
    }


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

}
