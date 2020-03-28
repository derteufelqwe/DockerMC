package de.derteufelqwe.ServerManager.config.configs;

import de.derteufelqwe.ServerManager.config.configs.objects.BungeeProxy;
import de.derteufelqwe.ServerManager.config.configs.objects.LobbyServerPool;
import de.derteufelqwe.ServerManager.config.configs.objects.MinecraftServer;
import de.derteufelqwe.ServerManager.config.configs.objects.ServerPool;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class InfrastructureConfig {

    public InfrastructureConfig() {
    }

    // BungeeCord servers
    private BungeeProxy proxy;

    private LobbyServerPool lobbyPool;

    // Servers, which have multiple replicates
    private List<ServerPool> poolServers = new ArrayList<>();

    // Single servers
    private List<MinecraftServer> servers = new ArrayList<>();

}
