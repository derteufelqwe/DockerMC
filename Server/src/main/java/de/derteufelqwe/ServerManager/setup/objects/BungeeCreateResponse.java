package de.derteufelqwe.ServerManager.setup.objects;

import com.github.dockerjava.api.command.InspectContainerResponse;

public class BungeeCreateResponse extends ResponseBase {

    public BungeeCreateResponse(String containerID) {
        super(containerID);
    }


    public boolean successful() {
        InspectContainerResponse response = this.docker.getDocker().inspectContainerCmd(this.containerID)
                .exec();

        return response.getState().getRunning() == null ? false : response.getState().getRunning();
    }

}
