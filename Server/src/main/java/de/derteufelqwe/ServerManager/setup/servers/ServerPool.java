package de.derteufelqwe.ServerManager.setup.servers;

import com.github.dockerjava.api.model.Driver;
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
        Map<String, String> containerLabels = super.getContainerLabels();

        containerLabels.putAll(Utils.quickLabel(Constants.ContainerType.MINECRAFT));
        containerLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return containerLabels;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        Map<String, String> labels = super.getServiceLabels();

        labels.putAll(Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL));
        labels.put(Constants.SERVER_NAME_KEY, this.name);
        labels.put("SOFT_PLAYER_LIMIT", String.valueOf(this.softPlayerLimit));

        return labels;
    }

    @Override
    protected List<String> getEnvs() {
        List<String> envs = super.getEnvs();

        envs.add("SERVER_NAME=" + this.name);
        envs.add("SOFT_PLAYER_LIMIT=" + this.softPlayerLimit);

        return envs;
    }

    @Override
    protected Driver getLogDriver() {
        return new Driver()
                .withName(Constants.DOCKER_DRIVER_PLUGIN_NAME);
    }


}
