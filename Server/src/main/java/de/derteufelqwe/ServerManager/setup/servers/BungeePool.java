package de.derteufelqwe.ServerManager.setup.servers;

import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.setup.templates.ExposableServiceTemplate;
import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.templates.ServiceTemplate;
import de.derteufelqwe.commons.Constants;
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

    public BungeePool(String name, String image, String ramLimit, String cpuLimit, int replications, ServiceConstraints constraints, int port) {
        super(name, image, ramLimit, cpuLimit, replications, constraints, port);
    }


    // -----  Creation methods  -----

    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> containerLabels = Utils.quickLabel(Constants.ContainerType.BUNGEE);
        containerLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return containerLabels;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        Map<String, String> serviceLabels = Utils.quickLabel(Constants.ContainerType.BUNGEE_POOL);
        serviceLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return serviceLabels;
    }

    @Override
    protected List<String> getEnvs() {
        List<String> envs = super.getEnvs();

        envs.add("TASK_NAME={{ .Task.Name }}");

        return envs;
    }
}
