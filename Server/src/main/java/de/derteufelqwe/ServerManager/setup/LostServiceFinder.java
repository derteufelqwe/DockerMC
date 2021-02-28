package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Identifies "lost" services.
 * A "lost" service is a Minecraft-Pool, which has no matching config anymore. This can happen when the config changes
 * or the pools get renamed.
 */
public class LostServiceFinder {

    private Docker docker;
    private ServersConfig serversConfig = ServerManager.SERVERS_CONFIG.get();

    public LostServiceFinder(Docker docker) {
        this.docker = docker;
    }


    /**
     * Returns all running docker services, which have the required labels.
     */
    private List<Service> getRelevantServices() {
        Map<String, String> labels1 = de.derteufelqwe.commons.Utils.quickLabel(Constants.ContainerType.BUNGEE_POOL);
        Map<String, String> labels2 = de.derteufelqwe.commons.Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL);
        Map<String, String> labels3 = Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL_PERSISTENT);

        List<Service> existingServices = this.docker.getDocker().listServicesCmd().withLabelFilter(labels1).exec();

        existingServices.addAll(this.docker.getDocker().listServicesCmd().withLabelFilter(labels1).exec());
        existingServices.addAll(this.docker.getDocker().listServicesCmd().withLabelFilter(labels2).exec());
        existingServices.addAll(this.docker.getDocker().listServicesCmd().withLabelFilter(labels3).exec());

        return existingServices;
    }

    /**
     * Returns all service names from the InfrastructureConfig.yml
     */
    private List<String> getConfiguredServiceNames() {
        List<String> names = new ArrayList<>();

        if (this.serversConfig.getBungeePool() != null) {
            names.add(this.serversConfig.getBungeePool().getName());
        }

        if (this.serversConfig.getLobbyPool() != null) {
            names.add(this.serversConfig.getLobbyPool().getName());
        }

        for (ServerPool pool : this.serversConfig.getPoolServers()) {
            names.add(pool.getName());
        }

        return names;
    }

    /**
     * Identifies lost services. A service is lost if it's labeled as a configurable server but not specified by any config.
     */
    public List<Service> findLostServices() {
        List<Service> existingServices = this.getRelevantServices();
        List<String> configuredServicesNames = this.getConfiguredServiceNames();

        for (Service service : new ArrayList<>(existingServices)) {
            if (configuredServicesNames.contains(service.getSpec().getName())) {
                existingServices.remove(service);
            }
        }

        return existingServices;
    }


}
