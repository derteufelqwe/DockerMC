package de.derteufelqwe.ServerManager.setup.infrastructure;

import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.setup.templates.ExposableServiceTemplate;
import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.commons.Constants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;


/**
 * Represents a docker service, which provides Nginx reverse proxy instances.
 * These are used to connect the player to the BungeeCord instances.
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NginxService extends ExposableServiceTemplate {

    private final int NGINX_INTERNAL_PORT = 25577;


    public NginxService(String name, String image, String ramLimit, String cpuLimit, int replications, ServiceConstraints constraints, int port) {
        super(name, image, ramLimit, cpuLimit, replications, constraints, port);
    }


    // -----  Creation methods  -----

    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> containerLabels = Utils.quickLabel(Constants.ContainerType.NGINX);
        containerLabels.put("TASK_NAME", "{{ .Task.Name }}");

        return containerLabels;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        Map<String, String> containerLabels = Utils.quickLabel(Constants.ContainerType.NGINX_POOL);

        return containerLabels;
    }

    @Override
    protected int getContainerPort() {
        return this.NGINX_INTERNAL_PORT;
    }

    @Override
    protected List<String> getEnvs() {
        List<String> envs = super.getEnvs();

        envs.add("TASK_NAME={{ .Task.Name }}");
        envs.add("NETWORK_ADDR=" + Constants.SUBNET_OVERNET.split("/")[0]);

        return envs;
    }
}
