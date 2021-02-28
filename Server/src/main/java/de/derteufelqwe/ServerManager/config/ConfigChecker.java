package de.derteufelqwe.ServerManager.config;

import de.derteufelqwe.ServerManager.exceptions.InvalidConfigException;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

/**
 * Validates the configs
 */
public class ConfigChecker {
    
    public ConfigChecker() {
    }


    /**
     * Validates the infrastructure config
     */
    public void validateInfrastructureConfig(ServersConfig config) throws InvalidConfigException {
        this.validateNamesNotDoubling(config);

        if (config.getBungeePool() != null) {
            config.getBungeePool().valid();
        }

        if (config.getLobbyPool() != null) {
            config.getLobbyPool().valid();
        }

        for (ServerPool pool : config.getPoolServers()) {
            pool.valid();
        }

    }


    /**
     * Validates that Server Names don't double
     */
    private void validateNamesNotDoubling(ServersConfig config) throws InvalidConfigException {
        Bag<String> names = new HashBag<>();

        if (config.getBungeePool() != null) {
            names.add(config.getBungeePool().getName());
        }

        if (config.getLobbyPool() != null) {
            names.add(config.getLobbyPool().getName());
        }

        for (ServerPool pool : config.getPoolServers()) {
            names.add(pool.getName());
        }

        for (String name : names) {
            int count = names.getCount(name);

            if (count > 1) {
                throw new InvalidConfigException("Server names must be unique. (%s).", name);
            }
        }
    }


}
