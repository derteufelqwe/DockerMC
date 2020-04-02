package de.derteufelqwe.ServerManager.setup.servers.responses;

import com.github.dockerjava.api.command.InspectContainerResponse;

/**
 * Response after creating a Server
 */
public class ServerResponse extends Response {


    public ServerResponse(String containerID) {
        super(containerID);
    }

    @Override
    public boolean successful() {
        this.failureCause = FailureCause.NOT_FAILED;
        InspectContainerResponse response = this.docker.getDocker().inspectContainerCmd(this.objectID)
                .exec();

        boolean isSuccessful = response.getState().getRunning() == null ? false : response.getState().getRunning();
        if (!isSuccessful) {
            this.failureCause = FailureCause.CONTAINER_FAIL;
        }

        return isSuccessful;
    }

    @Override
    public String getLogs() {
        return this.docker.getContainerLog(this.objectID);
    }

}
