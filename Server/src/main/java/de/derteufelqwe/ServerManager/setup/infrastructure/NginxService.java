package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.command.CreateServiceResponse;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.ServiceTemplate;
import de.derteufelqwe.ServerManager.setup.servers.ServerTemplate;
import de.derteufelqwe.commons.Constants;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NginxService extends ServerTemplate {

    // Port of the proxy
    private int port;

    public NginxService(String image, String ramLimit, String cpuLimit, String name, int replications, ServiceConstraints constraints, int port) {
        super(image, ramLimit, cpuLimit, name, replications, constraints);
        this.port = port;
    }

    @Override
    public ValidationResponse valid() {
        ValidationResponse response = new ValidationResponse(true, this.name, "");
        List<String> nullParams = this.validateParamsNotNull();

        if (this.port == 0) {
            nullParams.add("port");
        }

        if (nullParams.size() != 0) {
            response.setValid(false);
            response.setReason(response.getReason() +
                    "Parameters " + StringUtils.join(nullParams, ", ") + " can't be null.\n"
            );
        }

        try {
            Utils.convertMemoryString(this.ramLimit);

        } catch (FatalDockerMCError e) {
            response.setValid(false);
            response.setReason(response.getReason() +
                    "Ram constraint value " + this.ramLimit + " is unkown.\n"
            );
        }

        return response;
    }

    @Override
    public FindResponse find() {
        List<Service> services = this.docker.getDocker().listServicesCmd()
                .withLabelFilter(this.getServiceLabels())
                .exec();

        if (services.size() > 1) {
            throw new FatalDockerMCError("Found multiple services %s for the config %s.",
                    services.stream().map(Service::getId).collect(Collectors.joining(", ")), this.name);

        } else if (services.size() == 1) {
            return new ServerTemplate.FindResponse(true, services.get(0).getId());

        } else {
            return new ServerTemplate.FindResponse(false, null);
        }
    }

    @Override
    public CreateResponse create() {
        ServiceSpec serviceSpec = this.getServiceSpec();

        CreateServiceResponse serviceResponse = docker.getDocker().createServiceCmd(serviceSpec)
                .withAuthConfig(new AuthConfig()
                        .withUsername(this.mainConfig.getRegistryUsername())
                        .withPassword(this.mainConfig.getRegistryPassword()))
                .exec();

        return new ServerTemplate.CreateResponse(true, serviceResponse.getId());
    }

    @Override
    public DestroyResponse destroy() {
        FindResponse findResponse = this.find();

        if (findResponse.isFound()) {
            this.docker.getDocker().removeServiceCmd(findResponse.getServiceID()).exec();
            return new ServerTemplate.DestroyResponse(true, findResponse.getServiceID());
        }

        return new ServerTemplate.DestroyResponse(false, null);
    }

    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> containerLabels = Utils.quickLabel(Constants.ContainerType.NGINX);
        containerLabels.put("TASK_NAME", "{{ .Task.Name }}");

        return containerLabels;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        Map<String, String> containerLabels = Utils.quickLabel(Constants.ContainerType.NGINX_POOL);

        return containerLabels;
    }

    @Override
    protected List<PortConfig> getPortList() {
        List<PortConfig> portList = super.getPortList();

        portList.add(
                new PortConfig().withProtocol(PortConfigProtocol.TCP)
                        .withPublishedPort(this.port)
                        .withTargetPort(25577)
        );

        return portList;
    }

}
