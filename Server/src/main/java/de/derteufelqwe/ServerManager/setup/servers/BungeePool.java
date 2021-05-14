package de.derteufelqwe.ServerManager.setup.servers;

import com.github.dockerjava.api.model.Driver;
import de.derteufelqwe.ServerManager.setup.templates.ExposableServiceTemplate;
import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;


/**
 * Represents a docker service, which provides BungeeCord instances
 * These are used to connect the players to the Minecraft servers.
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BungeePool extends ExposableServiceTemplate {

    public BungeePool(String name, String image, String ramLimit, float cpuLimit, int replications, ServiceConstraints constraints, int port) {
        super(name, image, ramLimit, cpuLimit, replications, constraints, port);
    }

// -----  Creation methods  -----

    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> containerLabels = super.getContainerLabels();

        containerLabels.putAll(Utils.quickLabel(Constants.ContainerType.BUNGEE));
        containerLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return containerLabels;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        Map<String, String> serviceLabels = super.getServiceLabels();

        serviceLabels.putAll(Utils.quickLabel(Constants.ContainerType.BUNGEE_POOL));
        serviceLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return serviceLabels;
    }

    @Override
    protected List<String> getEnvs() {
        List<String> envs = super.getEnvs();

        return envs;
    }

    @Override
    protected Driver getLogDriver() {
        return new Driver()
                .withName(Constants.DOCKER_DRIVER_PLUGIN_NAME);
    }

}
