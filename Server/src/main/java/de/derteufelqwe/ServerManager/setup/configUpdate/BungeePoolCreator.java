package de.derteufelqwe.ServerManager.setup.configUpdate;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.setup.ConfigCreator;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;

public class BungeePoolCreator extends ConfigCreator<BungeePool> {

    public BungeePoolCreator(Docker docker) {
        super(
                ServerManager.getServerConfig().get().getBungeePool(),
                ServerManager.getServerConfigOld().get().getBungeePool(),
                docker,
                Constants.ContainerType.BUNGEE
        );
    }

    @Override
    protected void updateOldConfigFile(BungeePool newConfig) {
        Config<ServersConfig> serversConfig = ServerManager.getServerConfigOld();
        serversConfig.get().setBungeePool(newConfig);
        serversConfig.save();
    }

    @Override
    protected int getParallelUpdateCount() {
        MainConfig mainConfig = ServerManager.getMainConfig().get();
        return mainConfig.getBungeePoolParallelUpdates();
    }
}
