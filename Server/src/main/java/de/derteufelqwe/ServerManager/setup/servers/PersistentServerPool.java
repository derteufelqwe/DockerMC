package de.derteufelqwe.ServerManager.setup.servers;

import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.commons.Constants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;


/**
 * Represents Minecraft server pools, which provide a fixed number of server instances with PERSISTENT data.
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PersistentServerPool extends ServerPool {


    public PersistentServerPool(String name, String image, String ramLimit, String cpuLimit, int replications, ServiceConstraints constraints, int softPlayerLimit) {
        super(name, image, ramLimit, cpuLimit, replications, constraints, softPlayerLimit);
    }


    // -----  Creation methods  -----

    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> containerLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT_PERSISTENT);
        containerLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return containerLabels;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        Map<String, String> serviceLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL_PERSISTENT);
        serviceLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return serviceLabels;
    }

}
