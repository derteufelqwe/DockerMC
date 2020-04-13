package de.derteufelqwe.ServerManager.setup.servers.responses;

import com.github.dockerjava.api.model.Container;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.configs.objects.ServerObjBase;
import de.derteufelqwe.ServerManager.exceptions.InvalidStateError;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Needs to implement public Response create(config)
 * Needs to implement public String findService(config)
 */
public abstract class Response {

    protected FailureCause failureCause = null;
    @Getter protected String objectID;
    protected Docker docker;
    protected ServerObjBase configObj;
    protected List<Container> failedContainers = new ArrayList<>();


    public Response(String objectID, ServerObjBase configObj) {
        this.objectID = objectID;
        this.docker = ServerManager.getDocker();
        this.configObj = configObj;
    }


    public abstract boolean successful();


    public abstract String getLogs();


    public FailureCause getCause() {
        if (this.failureCause == null) {
            throw new InvalidStateError("State of response of serviceId '%s' is invalid. Execute #successful() first.",
                    this.objectID);
        }

        return this.failureCause;
    }


    public ServerObjBase getConfig() {
        return this.configObj;
    }

    /**
     * Returns a list of all containers that failed to start
     */
    public List<Container> getFailedContainers() {
        return new ArrayList<>(this.failedContainers);
    }

}
