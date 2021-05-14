package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.ServerManager.setup.templates.ServiceTemplate;

/**
 * Indicates that a ServiceConfig gets loaded by a config file a
 */
public interface IConfigFileBased<CFG extends ServiceTemplate> {

    public ServiceCreateResponse createOrUpdate(boolean force);

    default public ServiceCreateResponse createOrUpdate() {
        return createOrUpdate(false);
    }

}
