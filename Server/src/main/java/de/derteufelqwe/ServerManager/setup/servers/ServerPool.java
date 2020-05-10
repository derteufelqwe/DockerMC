package de.derteufelqwe.ServerManager.setup.servers;

import com.orbitz.consul.KeyValueClient;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.setup.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.ServiceTemplate;
import de.derteufelqwe.commons.Constants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
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

    public ServerPool(String name, String image, String ramLimit, String cpuLimit, int replications, ServiceConstraints constraints, int softPlayerLimit) {
        super(name, image, ramLimit, cpuLimit, replications, constraints);
        this.softPlayerLimit = softPlayerLimit;
    }


    public CreateResponse create(KeyValueClient kvClient) {
        CreateResponse response = super.create();

        kvClient.putValue("mcservers/" + this.name + "/softPlayerLimit", Integer.toString(this.softPlayerLimit));

        return response;
    }


    public DestroyResponse destroy(KeyValueClient kvClient) {
        DestroyResponse response = super.destroy();

        kvClient.deleteKey("mcservers/" + this.name);

        return response;
    }

    @Override
    protected List<String> findNullParams() {
        List<String> nullParams = super.findNullParams();

        if (this.softPlayerLimit == 0) {
            nullParams.add("softPlayerLimit");
        }

        return nullParams;
    }


    // -----  Creation methods  -----

    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> containerLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT);
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
        envs.add("");

        return envs;
    }

}
