package de.derteufelqwe.ServerManager.config.configs;

import de.derteufelqwe.ServerManager.config.configs.objects.*;
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

    // Lobby server
    private ServerPool lobbyPool = new ServerPool();

    // Servers, which have multiple replicates
    private List<ServerPool> poolServers = new ArrayList<>();

    // Single servers
    private List<MinecraftServer> servers = new ArrayList<>();

    // Multiple servers, persistent
    private List<PersistentServerPool> persistentServerPool = new ArrayList<>();

    // Single persistent server
    private PersistentMinecraftServer persistentServers;

}
