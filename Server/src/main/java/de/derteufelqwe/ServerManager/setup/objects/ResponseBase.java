package de.derteufelqwe.ServerManager.setup.objects;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for container responses
 */
public abstract class ResponseBase {

    @Getter
    protected String containerID;
    protected Docker docker;

    public ResponseBase(String containerID) {
        this.docker = ServerManager.getDocker();
        this.containerID = containerID;
    }


    public String getLogs() {
        return this.docker.getContainerLog(containerID);
    }


}
