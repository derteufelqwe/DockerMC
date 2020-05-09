package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.backend.Ignore;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import lombok.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Template to create Docker objects like Containers or Services
 */
@Data
@NoArgsConstructor
public abstract class DockerObjTemplate {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Ignore
    protected Docker docker;

    // Name
    protected String name;
    // Image
    protected String image;
    // RAM limit per task, like 2G or 512M
    protected String ramLimit;
    // CPU limit per task like 1
    protected String cpuLimit;


    public DockerObjTemplate(String name, String image, String ramLimit, String cpuLimit) {
        this.name = name;
        this.image = image;
        this.ramLimit = ramLimit;
        this.cpuLimit = cpuLimit;
    }

    public DockerObjTemplate(Docker docker) {
        this.docker = docker;
    }


    /**
     * Initialize the instance with a working Docker instance.
     * If this method doesn't get called before executing any other methods, the other methods will fail.
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
    public ValidationResponse valid() {
        ValidationResponse response = new ValidationResponse(true, this.name, "");
        List<String> nullParams = this.findNullParams();


        if (nullParams.size() != 0) {
            response.setValid(false);
            response.addToReason(
                    "Parameters " + StringUtils.join(nullParams, ", ") + " can't be null.\n"
            );
        }

        try {
            Utils.convertMemoryString(this.ramLimit);

        } catch (FatalDockerMCError e) {
            response.setValid(false);
            response.addToReason(
                    "RAM constraint value " + this.ramLimit + " is unknown.\n"
            );
        }

        if (this.cpuLimit != null && (this.cpuLimit.equals("0") || this.cpuLimit.equals("0.0"))) {
            response.setValid(false);
            response.addToReason(
                    "CPU constraint value can't be 0.\n"
            );
        }

        return response;
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

    /**
     * Response to the valid() function.
     */
    @Data
    @AllArgsConstructor
    public static class ValidationResponse {

        private boolean valid;
        private String name;
        private String reason;

        public void addToReason(String toAdd) {
            this.reason = this.reason + toAdd;
        }

    }


    // -----  Other methods  -----

    /**
     * Basic validation if parameters are not null.
     * @return List with all parameter names that are null.
     */
    protected List<String> findNullParams() {
        List<String> resultList = new ArrayList<>();

        if (this.name == null) {
            resultList.add("name");
        }
        if (this.image == null) {
            resultList.add("image");
        }
        if (this.ramLimit == null) {
            resultList.add("ramLimit");
        }
        if (this.cpuLimit == null) {
            resultList.add("cpuLimit");
        }

        return resultList;
    }

}
