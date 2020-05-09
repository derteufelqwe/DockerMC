package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.command.CreateServiceResponse;
import com.github.dockerjava.api.model.PortConfig;
import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.ServiceTemplate;
import de.derteufelqwe.ServerManager.setup.servers.ServerTemplate;
import de.derteufelqwe.commons.Constants;

import java.util.*;
import java.util.stream.Collectors;

public class ConsulService extends ServiceTemplate {

    public ConsulService() {
        super("Consul", "consul", "512M", "1", 1,
                new ServiceConstraints(null, null, Collections.singletonList("manager"), 0));
    }


    // -----  Creation methods  -----

    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> labels = super.getContainerLabels();

        labels.putAll(Utils.quickLabel(Constants.ContainerType.CONSUL));

        return labels;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        Map<String, String> labels = super.getServiceLabels();

        labels.putAll(Utils.quickLabel(Constants.ContainerType.CONSUL_POOL));

        return labels;
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

        command.addAll(Arrays.asList("agent", "-server", "-ui", "-node=server-1", "-bootstrap-expect=1", "-client=0.0.0.0", "-enable-script-checks"));

        return command;
    }

    @Override
    protected String getServiceName() {
        return "consul_server";
    }

    /**
     * Overwrite this because we don't want the consul image from the registry
     * @return
     */
    @Override
    protected String getImageName() {
        return this.image;
    }

}
