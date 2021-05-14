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

    public PersistentMinecraftPoolCreator(Docker docker, PersistentServerPool serverPool) {
        super(
                serverPool,
                getOldConfig(serverPool.getName()),
                docker,
                Constants.ContainerType.MINECRAFT
        );
    }

    private static PersistentServerPool getOldConfig(String name) {
        List<PersistentServerPool> relevantPools = ServerManager.getServerConfigOld().get().getPersistentServerPool().stream()
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
        Config<ServersConfig> serversConfig = ServerManager.getServerConfigOld();
        serversConfig.get().getPersistentServerPool().add(newConfig);
        serversConfig.save();
    }

    @Override
    protected int getParallelUpdateCount() {
        MainConfig mainConfig = ServerManager.getMainConfig().get();
        return mainConfig.getPoolParallelUpdates();
    }
}
