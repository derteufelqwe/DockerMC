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
    private BungeePool bungeePool;

    // Lobby server
    private ServerPool lobbyPool;

    // Servers, which have multiple replicates
    private List<ServerPool> poolServers = new ArrayList<>();

    // Multiple servers, persistent
    private List<PersistentServerPool> persistentServerPool = new ArrayList<>();


}
