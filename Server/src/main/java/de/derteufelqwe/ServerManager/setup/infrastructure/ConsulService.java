package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.command.CreateServiceResponse;
import com.github.dockerjava.api.model.PortConfig;
import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.ServiceTemplate;
import de.derteufelqwe.ServerManager.setup.servers.ServerTemplate;
import de.derteufelqwe.commons.Constants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConsulService extends ServiceTemplate {

    public ConsulService(Docker docker) {
        super("consul", "512M", "1");
        this.init(docker);
    }

    @Override
    public FindResponse find() {
        List<Service> services = this.docker.getDocker().listServicesCmd()
                .withLabelFilter(this.getServiceLabels())
                .exec();

        if (services.size() > 1) {
            throw new FatalDockerMCError("Found multiple services %s for the consul service.",
                    services.stream().map(Service::getId).collect(Collectors.joining(", ")));

        } else if (services.size() == 1) {
            return new ServerTemplate.FindResponse(true, services.get(0).getId());

        } else {
            return new ServerTemplate.FindResponse(false, null);
        }
    }

    @Override
    public CreateResponse create() {
        CreateServiceResponse response = docker.getDocker().createServiceCmd(this.getServiceSpec()).exec();

        return new CreateResponse(true, response.getId());
    }

    @Override
    public DestroyResponse destroy() {
        FindResponse findResponse = this.find();

        if (findResponse.isFound()) {
            docker.getDocker().removeServiceCmd(findResponse.getServiceID()).exec();

            return new DestroyResponse(true, findResponse.getServiceID());
        }

        return new DestroyResponse(false, findResponse.getServiceID());
    }


    @Override
    protected Map<String, String> getContainerLabels() {
        return new HashMap<>();
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        return Utils.quickLabel(Constants.ContainerType.CONSUL);
    }

    @Override
    protected List<PortConfig> getPortList() {
        List<PortConfig> portList = super.getPortList();

        portList.add(new PortConfig().withPublishedPort(8500).withTargetPort(8500));

        return portList;
    }

    @Override
    protected List<String> getEnvs() {
        List<String> envs = super.getEnvs();

        envs.add("CONSUL_BIND_INTERFACE=eth0");

        return envs;
    }

    @Override
    protected List<String> getCommandArgs() {
        List<String> command = super.getCommandArgs();

        command.addAll(Arrays.asList("agent", "-server", "-ui", "-node=server-1", "-bootstrap-expect=1", "-client=0.0.0.0"));

        return command;
    }

    @Override
    protected String getServiceName() {
        return "consul_server";
    }

    @Override
    protected String getImageName() {
        return this.image;
    }

}
