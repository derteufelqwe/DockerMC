package de.derteufelqwe.ServerManager.setup.servers;

import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.commons.Constants;

import java.util.List;
import java.util.Map;
import java.util.logging.StreamHandler;

public class MCServerDestroyer {

    private Docker docker;

    public MCServerDestroyer() {
        this.docker = ServerManager.getDocker();
    }


    /**
     * Destroys a Server pool by its name. Works for all types of servers
     * @param serverName
     * @return
     */
    public boolean destroy(String serverName) {
        boolean state1 = this.destroyBase(serverName, Constants.ContainerType.MINECRAFT_POOL);
        boolean state2 = this.destroyBase(serverName, Constants.ContainerType.MINECRAFT_POOL_PERSISTENT);

        return state1 || state2;
    }

    /**
     * Base method for destroying a server pool
     * @param serverName Server name to kill
     * @param containerType ContainerType to filter all services by. MINECRAFT_POOL for normal pools,
     *                      PERSISTENT_MINECRAFT_POOL for persistent servers
     * @return
     */
    private boolean destroyBase(String serverName, Constants.ContainerType containerType) {
        Map<String, String> labels = Utils.quickLabel(containerType);
        labels.put(Constants.SERVER_NAME_KEY, serverName);

        List<Service> services = this.docker.getDocker().listServicesCmd()
                .withLabelFilter(labels)
                .exec();

        for (Service service : services) {
            this.docker.getDocker().removeServiceCmd(service.getId()).exec();
        }

        return services.size() > 0;
    }



}
