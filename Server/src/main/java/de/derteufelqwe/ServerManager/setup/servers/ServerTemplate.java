package de.derteufelqwe.ServerManager.setup.servers;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.ServiceTemplate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;


/**
 * Template to create a Minecraft server, where players can join
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ServerTemplate extends ServiceTemplate {


    public ServerTemplate(String name, String image, String ramLimit, String cpuLimit, int replications, ServiceConstraints constraints) {
        super(name, image, ramLimit, cpuLimit, replications, constraints);
    }

    public ServerTemplate(Docker docker) {
        super(docker);
    }


    // -----  Utility methods  -----

    @Override
    protected List<String> getEnvs() {
        List<String> envs = super.getEnvs();

        envs.add("TASK_NAME={{ .Task.Name }}");
        envs.add("SERVER_NAME=" + this.name);

        return envs;
    }
}
