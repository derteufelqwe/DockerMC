package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Identifies and removed "lost" services.
 * A "lost" service is a Minecraft-Pool, which has no matching config anymore. This can happen when the config changes
 * and pools get renamed.
 */
public class LostServiceFinder {

    private Docker docker;
    private InfrastructureConfig infrastructureConfig = ServerManager.CONFIG.get(InfrastructureConfig.class);

    public LostServiceFinder(Docker docker) {
        this.docker = docker;
    }


    /**
     * Returns all running docker services, which have the required labels.
     */
    private List<Service> getRelevantServices() {
        Map<String, String> labels1 = Utils.quickLabel(Constants.ContainerType.BUNGEE_POOL);
        Map<String, String> labels2 = Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL);
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

        if (this.infrastructureConfig.getBungeePool() != null) {
            names.add(this.infrastructureConfig.getBungeePool().getName());
        }

        if (this.infrastructureConfig.getLobbyPool() != null) {
            names.add(this.infrastructureConfig.getLobbyPool().getName());
        }

        for (ServerPool pool : this.infrastructureConfig.getPoolServers()) {
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
