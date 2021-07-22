package de.derteufelqwe.ServerManager.setup.configUpdate;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.setup.ConfigCreator;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;

public class BungeePoolCreator extends ConfigCreator<BungeePool> {

    @Inject
    public BungeePoolCreator(Docker docker, Config<MainConfig> mainConfig, @Named("current") Config<ServersConfig> serversConfig,
                             @Named("old") Config<ServersConfig> serversConfigOld) {
        super(mainConfig,
                serversConfig,
                serversConfigOld,
                serversConfig.get().getBungeePool(),
                serversConfigOld.get().getBungeePool(),
                docker,
                Constants.ContainerType.BUNGEE
        );
    }

    @Override
    protected void updateOldConfigFile(BungeePool newConfig) {
        serversConfigOld.get().setBungeePool(newConfig);
        serversConfigOld.save();
    }

    @Override
    protected int getParallelUpdateCount() {
        return mainConfig.get().getBungeePoolParallelUpdates();
    }
}
