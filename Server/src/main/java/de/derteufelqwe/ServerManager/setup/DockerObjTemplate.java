package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.config.backend.Ignore;
import lombok.*;

@Data
@NoArgsConstructor
public abstract class DockerObjTemplate {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Ignore
    protected Docker docker;

    // Image
    protected String image;
    // RAM limit per task, like 2G or 512M
    protected String ramLimit;
    // CPU limit per task like 1
    protected String cpuLimit;


    public DockerObjTemplate(String image, String ramLimit, String cpuLimit) {
        this.image = image;
        this.ramLimit = ramLimit;
        this.cpuLimit = cpuLimit;
    }

    public DockerObjTemplate(Docker docker) {
        this.docker = docker;
    }


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


    // -----  Responses  -----

    /**
     * Response to the find() function.
     */
    @Data
    @AllArgsConstructor
    public class FindResponse {

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
    public class CreateResponse {

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
    public class DestroyResponse {

        private boolean destroyed;
        private String serviceID;
        private String message;

        public DestroyResponse(boolean destroyed, String serviceID) {
            this.destroyed = destroyed;
            this.serviceID = serviceID;
        }

    }

}
