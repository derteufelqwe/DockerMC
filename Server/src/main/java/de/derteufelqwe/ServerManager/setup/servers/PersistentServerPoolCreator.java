package de.derteufelqwe.ServerManager.setup.servers;

import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.configs.objects.PersistentServerPool;
import de.derteufelqwe.ServerManager.config.configs.objects.ServerPool;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.servers.responses.Response;
import de.derteufelqwe.commons.Constants;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PersistentServerPoolCreator extends CreatorBase {


    public Response create(PersistentServerPool poolConfig) {
        return this.createService(poolConfig);
    }


    private Response createService(PersistentServerPool poolConfig) {
        return null;
    }


    /**
     * Tries to find an existing instance of the config
     * @param poolConfig Config to search for
     * @return The service id if found, otherwise null
     */
    public String findService(PersistentServerPool poolConfig) {
        // Normal labels + name of the server (for Service)
        Map<String, String> serviceLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL_PERSISTENT);
        serviceLabels.put(Constants.SERVER_NAME_KEY, poolConfig.getName());

        List<Service> services = this.docker.getDocker().listServicesCmd()
                .withLabelFilter(serviceLabels)
                .exec();

        if (services.size() > 1) {
            throw new FatalDockerMCError("Found multiple services %s for the config %s.",
                    services.stream().map(Service::getId).collect(Collectors.joining(", ")), poolConfig.getName());

        } else if (services.size() == 1) {
            return services.get(0).getId();

        } else {
            return null;
        }
    }

}
