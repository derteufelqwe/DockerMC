package de.derteufelqwe.ServerManager.setup.servers;

import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.templates.ServiceTemplate;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;


/**
 * Represents Minecraft server pools, which provide a fixed number of server instances with VOLITILE data.
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ServerPool extends ServiceTemplate {

    // Soft playerlimit
    private int softPlayerLimit;

    public ServerPool(String name, String image, String ramLimit, float cpuLimit, int replications, ServiceConstraints constraints, int softPlayerLimit) {
        super(name, image, ramLimit, cpuLimit, replications, constraints);
        this.softPlayerLimit = softPlayerLimit;
    }


    // -----  Creation methods  -----

    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> containerLabels = de.derteufelqwe.commons.Utils.quickLabel(Constants.ContainerType.MINECRAFT);
        containerLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return containerLabels;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        Map<String, String> serviceLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL);
        serviceLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return serviceLabels;
    }

    @Override
    protected List<String> getEnvs() {
        List<String> envs = super.getEnvs();

        envs.add("TASK_NAME={{ .Task.Name }}");
        envs.add("SERVER_NAME=" + this.name);
        envs.add("SOFT_PLAYER_LIMIT=" + this.softPlayerLimit);

        return envs;
    }

}
