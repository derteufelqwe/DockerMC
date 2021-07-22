package de.derteufelqwe.ServerManager.setup.configUpdate;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.ConfigCreator;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.stream.Collectors;

public class MinecraftPoolCreator extends ConfigCreator<ServerPool> {

    public MinecraftPoolCreator(Docker docker, ServerPool serverPool, Config<MainConfig> mainConfig, Config<ServersConfig> serversConfig,
                            Config<ServersConfig> serversConfigOld) {
        super(mainConfig,
                serversConfig,
                serversConfigOld,
                serverPool,
                getOldConfig(serverPool.getName(), serversConfigOld.get()),
                docker,
                Constants.ContainerType.MINECRAFT
        );
    }

    private static ServerPool getOldConfig(String name, ServersConfig serversConfigOld) {
        List<ServerPool> relevantPools = serversConfigOld.getPoolServers().stream()
                .filter(p -> p.getName().equals(name))
                .collect(Collectors.toList());

        if (relevantPools.size() == 0) {
            return null;

        } else if (relevantPools.size() == 1) {
            return relevantPools.get(0);

        } else {
            throw new FatalDockerMCError("Found multiple pools named %s in old config.", name);
        }
    }

    @Override
    protected void updateOldConfigFile(ServerPool newConfig) {
        serversConfigOld.get().getPoolServers().add(newConfig);
        serversConfigOld.save();
    }

    @Override
    protected int getParallelUpdateCount() {
        return mainConfig.get().getPoolParallelUpdates();
    }
}
