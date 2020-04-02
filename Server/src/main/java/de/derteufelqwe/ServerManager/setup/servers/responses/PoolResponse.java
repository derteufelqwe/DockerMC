package de.derteufelqwe.ServerManager.setup.servers.responses;

import com.github.dockerjava.api.model.Container;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.exceptions.TimeoutException;
import de.derteufelqwe.commons.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Response after creating a Server pool
 */
public class PoolResponse extends Response {

    private List<Container> failedContainers = new ArrayList<>();


    public PoolResponse(String containerID) {
        super(containerID);
    }

    @Override
    public boolean successful() {
        this.failedContainers.clear();
        this.failureCause = FailureCause.NOT_FAILED;
        Map<String, String> labels = Utils.quickLabel(Constants.ContainerType.MINECRAFT);
        labels.put("com.docker.swarm.service.id", this.objectID);

        // Wait for service to spawn its tasks
        try {
            this.docker.waitService(this.objectID, Constants.SERVICE_STARTUP_TIME); // Will ensure that all tasks are running

        } catch (TimeoutException e) {
            this.failureCause = FailureCause.SERVICE_NOT_STARTED;
            return false;
        }

        List<Container> containers = this.docker.getDocker().listContainersCmd()
                .withLabelFilter(labels)
                .exec();

        // Check if more than 0 containers got created
        if (containers.size() == 0) {
            this.failureCause = FailureCause.SERVICE_ZERO_TASKS;
            return false;
        }

        for (Container container : containers) {
            if (!this.checkSingleSuccessful(container)) {
                this.failedContainers.add(container);
            }
        }

        // Check if all containers started successfully
        boolean isSuccessful = this.failedContainers.size() == 0 && containers.size() > 0;
        if (!isSuccessful) {
            this.failureCause = FailureCause.CONTAINER_FAIL;
        }

        return isSuccessful;
    }

    /**
     * Checks if a single container is running
     *
     * @return Yes or no
     */
    private boolean checkSingleSuccessful(Container container) {

        return container.getState().equals("running");
    }

    /**
     * Returns a list of all containers that failed to start
     */
    public List<Container> getFailedContainers() {
        return new ArrayList<>(this.failedContainers);
    }

    @Override
    public String getLogs() {
        return this.docker.getServiceLog(this.objectID);
    }

}
