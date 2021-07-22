package de.derteufelqwe.ServerManager.setup.configUpdate;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.ConfigCreator;
import de.derteufelqwe.ServerManager.setup.servers.PersistentServerPool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

public class PersistentMinecraftPoolCreator extends ConfigCreator<PersistentServerPool> {

    public PersistentMinecraftPoolCreator(Docker docker, PersistentServerPool serverPool, Config<MainConfig> mainConfig, Config<ServersConfig> serversConfig,
                                Config<ServersConfig> serversConfigOld) {
        super(mainConfig,
                serversConfig,
                serversConfigOld,
                serverPool,
                getOldConfig(serverPool.getName(), serversConfigOld.get()),
                docker,
                Constants.ContainerType.MINECRAFT_PERSISTENT
        );
    }

    private static PersistentServerPool getOldConfig(String name, ServersConfig serversConfigOld) {
        List<PersistentServerPool> relevantPools = serversConfigOld.getPersistentServerPool().stream()
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
    protected void updateOldConfigFile(PersistentServerPool newConfig) {
        serversConfigOld.get().getPersistentServerPool().removeIf(p -> p.getName().equals(newConfig.getName()));
        serversConfigOld.get().getPersistentServerPool().add(newConfig);
        serversConfigOld.save();
    }

    @Override
    protected int getParallelUpdateCount() {
        return mainConfig.get().getPoolParallelUpdates();
    }
}
