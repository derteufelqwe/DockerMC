package de.derteufelqwe.ServerManager.setup.servers;

import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
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


    public PersistentServerPool(String name, String image, String ramLimit, float cpuLimit, int replications, ServiceConstraints constraints, int softPlayerLimit) {
        super(name, image, ramLimit, cpuLimit, replications, constraints, softPlayerLimit);
    }


    // -----  Creation methods  -----

    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> containerLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT_PERSISTENT);

        return containerLabels;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        Map<String, String> labels = Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL_PERSISTENT);

        return labels;
    }

}
