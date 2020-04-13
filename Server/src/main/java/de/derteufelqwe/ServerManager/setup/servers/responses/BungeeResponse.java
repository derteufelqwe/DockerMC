package de.derteufelqwe.ServerManager.setup.servers.responses;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import de.derteufelqwe.ServerManager.config.configs.objects.BungeeProxy;

import java.util.Collections;

public class BungeeResponse extends Response {

    public BungeeResponse(String containerID, BungeeProxy configObj) {
        super(containerID, configObj);
        this.configObj = configObj;
    }


    public boolean successful() {
        this.failureCause = null;
        InspectContainerResponse response = this.docker.getDocker().inspectContainerCmd(this.objectID)
                .exec();

        boolean isSuccessful = response.getState().getRunning() == null ? false : response.getState().getRunning();
        if (!isSuccessful) {
            Container container = this.docker.getDocker().listContainersCmd()
                    .withShowAll(true)
                    .withIdFilter(Collections.singleton(this.objectID))
                    .exec().get(0);

            this.failedContainers.add(container);
            this.failureCause = FailureCause.CONTAINER_STARTUP_FAIL;
        }

        return isSuccessful;
    }

    @Override
    public String getLogs() {
        return this.docker.getContainerLog(this.objectID);
    }
}
