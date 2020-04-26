package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.ContainerTemplate;
import de.derteufelqwe.commons.Constants;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RegistryContainer extends ContainerTemplate {

    /**
     * Remove this Constructor from access. Use the constructor below instead
     */
    protected RegistryContainer(String image, String ramLimit, String cpuLimit) {
        super(image, ramLimit, cpuLimit);
    }

    public RegistryContainer(Docker docker) {
        super(Constants.Images.REGISTRY.image(), "1G", "2");
        this.init(docker);
    }


    @Override
    public FindResponse find() {
        List<Container> registryContainers = docker.getDocker().listContainersCmd()
                .withLabelFilter(Utils.quickLabel(Constants.ContainerType.REGISTRY))
                .exec();

        if (registryContainers.size() > 1) {
            throw new FatalDockerMCError("Found multiple Registry containers.");

        } else if (registryContainers.size() == 1) {
            return new FindResponse(true, registryContainers.get(0).getId());

        } else {
            return new FindResponse(false, null);
        }
    }

    @Override
    public CreateResponse create() {
        CreateContainerResponse response = docker.getDocker().createContainerCmd(this.image)
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


    @Override
    protected List<PortBinding> getPortBindings() {
        return Arrays.asList(
                new PortBinding(Ports.Binding.bindPort(443), ExposedPort.tcp(5000))
        );
    }

    @Override
    protected List<Bind> getBindMounts() {
        return Arrays.asList(
                new Bind("registry_data", new Volume("/var/lib/registry"), false),
                new Bind(Constants.REGISTRY_CERT_PATH,
                        new Volume("/auth")),
                new Bind(Constants.REGISTRY_CERT_PATH,
                        new Volume("/certs"))
        );
    }

    @Override
    protected List<String> getEnvironmentVariables() {
        return Arrays.asList("REGISTRY_AUTH=htpasswd", "REGISTRY_AUTH_HTPASSWD_REALM=Registry Realm",
                "REGISTRY_AUTH_HTPASSWD_PATH=/auth/" + Constants.REGISTRY_HTPASSWD_NAME,
                "REGISTRY_HTTP_TLS_CERTIFICATE=/certs/" + Constants.REGISTRY_CERT_NAME,
                "REGISTRY_HTTP_TLS_KEY=/certs/" + Constants.REGISTRY_KEY_NAME, "LOGLEVEL=INFO", "DEBUG=true"
        );
    }

    @Override
    protected Map<String, String> getContainerLabels() {
        return Utils.quickLabel(Constants.ContainerType.REGISTRY);
    }

}
