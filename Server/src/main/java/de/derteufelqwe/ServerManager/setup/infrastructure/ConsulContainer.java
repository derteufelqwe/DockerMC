package de.derteufelqwe.ServerManager.setup.infrastructure;

import de.derteufelqwe.ServerManager.setup.templates.ExposableContainerTemplate;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ConsulContainer extends ExposableContainerTemplate {


    public ConsulContainer() {
        super("consul", Constants.Images.CONSUL.image(), "512M", 0.5F, Constants.CONSUL_PORT);
    }


    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> labels = super.getContainerLabels();

        labels.putAll(Utils.quickLabel(Constants.ContainerType.CONSUL));

        return labels;
    }

    @Override
    protected List<String> getEnvironmentVariables() {
        List<String> envs = super.getEnvironmentVariables();

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
    protected List<String> getNetworks() {
        List<String> networks =  super.getNetworks();

        networks.add(Constants.NETW_OVERNET_NAME);

        return networks;
    }

    //    /**
//     * Overwrite this because we don't want the consul image from the registry
//     *
//     * @return
//     */
//    @Override
//    protected String getImageName() {
//        return this.image;
//    }

}
