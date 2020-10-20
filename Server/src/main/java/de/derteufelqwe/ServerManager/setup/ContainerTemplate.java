package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import lombok.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Template to create a docker container
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ContainerTemplate extends DockerObjTemplate {

    protected final int CONTAINER_START_DELAY = 10;   // Time for containers to get up and running.
    protected final int NETWORK_CREATE_DELAY  = 2;    // Time for networks to get up and running


    public ContainerTemplate(String name, String image, String ramLimit, String cpuLimit) {
        super(name, image, ramLimit, cpuLimit);
    }


    @Override
    public FindResponse find() {
        List<Container> container = docker.getDocker().listContainersCmd()
                .withLabelFilter(this.getContainerLabels())
                .exec();

        if (container.size() > 1) {
            throw new FatalDockerMCError("Found multiple containers for " + this.name + ".");

        } else if (container.size() == 1) {
            return new FindResponse(true, container.get(0).getId());

        } else {
            return new FindResponse(false, null);
        }
    }

    @Override
    public CreateResponse create() {
        docker.pullImage(this.image);

        CreateContainerResponse response = docker.getDocker().createContainerCmd(this.image)
                .withName(this.name)
                .withLabels(this.getContainerLabels())
                .withEnv(this.getEnvironmentVariables())
                .withHostConfig(this.getHostConfig())
                .exec();

        docker.getDocker().startContainerCmd(response.getId()).exec();

        String containerID = response.getId();
        WaitResponse waitResponse = this.waitForContainerStart(containerID);

        if (!waitResponse.isRunning()) {
            return new CreateResponse(false, containerID, waitResponse.getMessage());
        }

        return new CreateResponse(true, containerID);
    }

    @Override
    public DestroyResponse destroy() {
        FindResponse findResponse = this.find();

        if (findResponse.isFound()) {
            this.docker.getDocker().stopContainerCmd(findResponse.getServiceID()).exec();
            return new DestroyResponse(true, findResponse.getServiceID());
        }

        return new DestroyResponse(false, null);
    }


    // ----- Other methods  -----

    protected WaitResponse waitForContainerStart(String containerID) {
        try {
            docker.getDocker().waitContainerCmd(containerID)
                    .exec(new WaitContainerResultCallback())
                    .awaitStarted(CONTAINER_START_DELAY, TimeUnit.SECONDS);

            TimeUnit.SECONDS.sleep(1);

            InspectContainerResponse r = docker.getDocker().inspectContainerCmd(containerID)
                    .exec();

            boolean isRunning = r.getState().getRunning() == null ? false : r.getState().getRunning();
            return new WaitResponse(isRunning, containerID);

        } catch (InterruptedException e) {
            e.printStackTrace();
            return new WaitResponse(false, containerID, e.getMessage());
        }
    }


    // -----  Building methods  -----


    /**
     * Returns a list of the published ports
     */
    protected List<PortBinding> getPortBindings() {
        return new ArrayList<>();
    }

    /**
     * Returns a list of the host-bound mounts
     */
    protected List<Bind> getBindMounts() {
        return new ArrayList<>();
    }

    /**
     * Returns a list of the environment variables
     */
    protected List<String> getEnvironmentVariables() {
        return new ArrayList<>();
    }

    /**
     * Returns a map of the container labels
     */
    protected Map<String, String> getContainerLabels() {
        return new HashMap<>();
    }

    /**
     * Returns the HostConfig, setting the container memory and cpu limits
     */
    protected HostConfig getHostConfig() {
        long nanoCpu = (long) (Double.parseDouble(this.cpuLimit) * 1000000000);
        HostConfig hostConfig = new HostConfig()
                .withMemory(Utils.convertMemoryString(this.ramLimit))
                .withNanoCPUs(nanoCpu)
                .withBinds(this.getBindMounts())
                .withPortBindings(this.getPortBindings());

        return hostConfig;
    }


    // -----  Responses  -----

    /**
     * Response to the waitForContainerStart() function.
     */
    @Data
    @AllArgsConstructor
    public class WaitResponse {

        private boolean running;
        private String serviceID;
        private String message;

        public WaitResponse(boolean running, String serviceID) {
            this.running = running;
            this.serviceID = serviceID;
        }

    }

}
