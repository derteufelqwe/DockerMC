package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.setup.templates.ServiceTemplate;

public interface ConfigUpdater<CFG extends ServiceTemplate> {

    public void updateConfigFile(CFG newConfig);

}
