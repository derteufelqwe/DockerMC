package de.derteufelqwe.ServerManager.setup;

import com.orbitz.consul.KeyValueClient;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.SystemConfig;

/**
 * Responsible for updating the Services
 */
public class ServerConfigUpdater {

    /**
     * Idee:
     * 2. Server Config, die die "alte" Version beinhaltet
     *
     */

    private Docker docker;
    private KeyValueClient kvClient;
    private InfrastructureConfig infrastructureConfig = ServerManager.CONFIG.get(InfrastructureConfig.class);
    private SystemConfig systemConfig = ServerManager.CONFIG.get(SystemConfig.class);

    public ServerConfigUpdater(Docker docker, KeyValueClient kvClient) {
        this.docker = docker;
        this.kvClient = kvClient;
    }

    public void update() {

    }


    private boolean checkBungeePool() {
//        return this.infrastructureConfig.getBungeePool();
        return true;
    }


}
