package de.derteufelqwe.ServerManager.setup.templates;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.exceptions.InvalidConfigException;
import de.derteufelqwe.commons.config.annotations.Exclude;
import lombok.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Template to create Docker objects like Containers or Services
 */
@Data
@NoArgsConstructor
public abstract class DockerObjTemplate implements Cloneable {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Exclude
    protected Docker docker;

    // Name
    protected String name;
    // Image
    protected String image;
    // RAM limit per task, like 2G or 512M
    protected String ramLimit;
    // CPU limit per task like 1
    protected float cpuLimit;


    public DockerObjTemplate(String name, String image, String ramLimit, float cpuLimit) {
        this.name = name;
        this.image = image;
        this.ramLimit = ramLimit;
        this.cpuLimit = cpuLimit;
    }


    /**
     * Initialize the instance with a working Docker instance.
     * If this method doesn't get called before executing any other methods, the other methods will fail.
     * This method is required to set the docker instance to deserialized instances.
     *
     * @param docker Docker instance to set
     */
    public void init(Docker docker) {
        this.docker = docker;
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
     * Validates the config and checks whether the config is valid or not
     *
     * @return Returns a ValidationResponse with more information
     */
    public void valid() throws InvalidConfigException {
        // Name
        if (this.name == null) {
            throw new InvalidConfigException("ServerName can't be null.");
        }
        if (this.name.contains(" ")) {
            throw new InvalidConfigException("ServerName can't container whitespaces.");
        }

        // Image
        if (this.image == null) {
            throw new InvalidConfigException("Image name can't be null.");
        }
        if (this.image.startsWith("registry.swarm")) {
            throw new InvalidConfigException("Image name must start WITHOUT 'registry.swarm/'.");
        }
        if (this.image.contains(" ")) {
            throw new InvalidConfigException("Image name can't contain whitespaces.");
        }

        // Ram
        if (this.ramLimit == null) {
            throw new InvalidConfigException("RAM limit can't be null.");
        }
        try {
            Utils.convertMemoryString(this.ramLimit);
        } catch (FatalDockerMCError ignored) {
            throw new InvalidConfigException("RAM limit '%s' is unknown. Use K, M or G.");
        }

        // CPU
        if (this.cpuLimit <= 0F) {
            throw new InvalidConfigException("CPU limit can't be 0 or even negative.");
        }

    }


    // -----  Responses  -----

    /**
     * Response to the find() function.
     */
    @Data
    @AllArgsConstructor
    public static class FindResponse {

        private boolean found;
        private String serviceID;
        private String message;

        public FindResponse(boolean found, String serviceID) {
            this.found = found;
            this.serviceID = serviceID;
        }

    }

    /**
     * Response to the create() function.
     */
    @Data
    @AllArgsConstructor
    public static class CreateResponse {

        private boolean created;
        private String serviceID;
        private String message;

        public CreateResponse(boolean created, String serviceID) {
            this.created = created;
            this.serviceID = serviceID;
        }

    }

    /**
     * Response to the destroy() function.
     */
    @Data
    @AllArgsConstructor
    public static class DestroyResponse {

        private boolean destroyed;
        private String serviceID;
        private String message;

        public DestroyResponse(boolean destroyed, String serviceID) {
            this.destroyed = destroyed;
            this.serviceID = serviceID;
        }

    }


    // -----  Other methods  -----


    /**
     * Default Clone
     */
    @SneakyThrows
    @Override
    public DockerObjTemplate clone() {
        return (DockerObjTemplate) super.clone();
    }
}
