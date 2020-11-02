package de.derteufelqwe.ServerManager.config;

import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.exceptions.InvalidConfigException;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

/**
 * Validates the configs
 */
public class ConfigChecker {

    private InfrastructureConfig infrastructureConfig = ServerManager.CONFIG.get(InfrastructureConfig.class);


    public ConfigChecker() {
    }


    /**
     * Validates the infrastructure config
     */
    public void validateInfrastructureConfig() {
        this.validateNamesNotDoubling();

        if (this.infrastructureConfig.getBungeePool() != null) {
            this.infrastructureConfig.getBungeePool().valid();
        }

        if (this.infrastructureConfig.getLobbyPool() != null) {
            this.infrastructureConfig.getLobbyPool().valid();
        }

        for (ServerPool pool : this.infrastructureConfig.getPoolServers()) {
            pool.valid();
        }

    }


    /**
     * Validates that Server Names don't double
     */
    private void validateNamesNotDoubling() throws InvalidConfigException {
        Bag<String> names = new HashBag<>();

        if (this.infrastructureConfig.getBungeePool() != null) {
            names.add(this.infrastructureConfig.getBungeePool().getName());
        }

        if (this.infrastructureConfig.getLobbyPool() != null) {
            names.add(this.infrastructureConfig.getLobbyPool().getName());
        }

        for (ServerPool pool : this.infrastructureConfig.getPoolServers()) {
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
