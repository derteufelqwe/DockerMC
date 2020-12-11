package de.derteufelqwe.ServerManager.setup.infrastructure;

import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.setup.templates.ExposableContainerTemplate;
import de.derteufelqwe.commons.Constants;

import java.util.List;
import java.util.Map;

public class RedisContainer extends ExposableContainerTemplate {


    public RedisContainer() {
        super(Constants.REDIS_CONTAINER_NAME, Constants.Images.REDIS.image(), "512M", 0.5F, Constants.REDIS_PORT);
    }


    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> labels = super.getContainerLabels();

        labels.putAll(Utils.quickLabel(Constants.ContainerType.REDIS_DB));

        return labels;
    }

    @Override
    protected List<String> getEnvironmentVariables() {
        List<String> envs = super.getEnvironmentVariables();

        return envs;
    }

    @Override
    protected List<String> getNetworks() {
        List<String> networks = super.getNetworks();

        networks.add(Constants.NETW_OVERNET_NAME);

        return networks;
    }

}
