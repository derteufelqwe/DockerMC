package de.derteufelqwe.ServerManager.setup.servers;

import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.backend.Config;
import de.derteufelqwe.ServerManager.config.backend.Ignore;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.setup.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.ServiceTemplate;
import de.derteufelqwe.commons.Constants;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
