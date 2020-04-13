package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.ServerManager.config.Config;
import de.derteufelqwe.ServerManager.config.configs.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.configs.objects.BungeeProxy;
import de.derteufelqwe.ServerManager.config.configs.objects.ServerPool;
import de.derteufelqwe.ServerManager.exceptions.BadConfigError;
import de.derteufelqwe.ServerManager.setup.servers.BungeeProxyCreator;
import de.derteufelqwe.ServerManager.setup.servers.ServerPoolCreator;
import de.derteufelqwe.ServerManager.setup.servers.responses.BungeeResponse;
import de.derteufelqwe.ServerManager.setup.servers.responses.PoolResponse;

import java.util.ArrayList;
import java.util.List;

public class MCServerCreator {

    private InfrastructureConfig config;

    public MCServerCreator() {
        this.config = Config.get(InfrastructureConfig.class);
    }


    /**
     * Creates the BungeeCord proxy based on the config in the InfrastructureConfig
     * When no config is specified, returns a Response with the serviceID null
     * @return A list of BungeeResponses for every Service created (Can only be one).
     */
    public List<BungeeResponse> createBungeeProxy() {
        BungeeProxy proxyConfig = this.config.getProxy();
        List<BungeeResponse> responseList = new ArrayList<>();
        BungeeProxyCreator creator = new BungeeProxyCreator();

        if (proxyConfig == null) {
            System.out.println("Config for bungee proxy is null.");
            responseList.add(new BungeeResponse(null, proxyConfig));
            return responseList;
        }

        String sericeId = creator.findService(proxyConfig);
        if (sericeId != null) {
            System.out.println("Found existing service for ProxyServer " + proxyConfig.getName() + ".");
            responseList.add(new BungeeResponse(sericeId, proxyConfig));

        } else {
            System.out.println("Couldn't find service for ProxyServer " + proxyConfig.getName() + ". Creating it...");
            responseList.add((BungeeResponse) creator.create(proxyConfig));
        }

        return responseList;
    }

    /**
     * Creates the Lobby server based on the config in the InfrastructureConfig
     * When no config is specified, returns a Response with the serviceID null
     * @return A list of PoolResponse for every Service created (Can only be one).
     */
    public List<PoolResponse> createLobbyServers() {
        ServerPool lobbyConfig = this.config.getLobbyPool();
        List<PoolResponse> responseList = new ArrayList<>();
        ServerPoolCreator creator = new ServerPoolCreator();

        if (lobbyConfig == null) {
            System.out.println("Config for lobby server is null.");
            responseList.add(new PoolResponse("", lobbyConfig));
            return responseList;
        }

        String sericeId = creator.findService(lobbyConfig);
        if (sericeId != null) {
            System.out.println("Found existing service for LobbyServer " + lobbyConfig.getName() + ".");
            responseList.add(new PoolResponse(sericeId, lobbyConfig));

        } else {
            System.out.println("Couldn't find service for LobbyServer " + lobbyConfig.getName() + ". Creating it...");
            responseList.add((PoolResponse) creator.create(lobbyConfig));
        }

        return responseList;
    }

    /**
     * Creates the server pools based on the config in the InfrastructureConfig
     * When no config is specified, returns an empty list
     * @return A list of PoolResponse for every Service created.
     */
    public List<PoolResponse> createPoolServers() {
        List<ServerPool> serverPools = this.config.getPoolServers();
        List<PoolResponse> responseList = new ArrayList<>();
        ServerPoolCreator creator = new ServerPoolCreator();

        for (ServerPool poolCfg : serverPools) {
            String sericeId = creator.findService(poolCfg);
            if (sericeId != null) {
                System.out.println("Found existing service for PoolServer " + poolCfg.getName() + ".");
                responseList.add(new PoolResponse(sericeId, poolCfg));

            } else {
                System.out.println("Couldn't find service for PoolServer " + poolCfg.getName() + ". Creating it...");
                responseList.add((PoolResponse) creator.create(poolCfg));
            }
        }

        return responseList;
    }

}
