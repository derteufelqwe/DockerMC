package de.derteufelqwe.ServerManager.config;

import de.derteufelqwe.ServerManager.config.backend.YAMLComment;
import de.derteufelqwe.ServerManager.setup.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.infrastructure.NginxService;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.PersistentServerPool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class InfrastructureConfig {

    // Nginx server
    @YAMLComment("Nginx reverse proxy")
    private NginxService nginxService;

    // BungeeCord servers
    @YAMLComment("BungeeCord servers")
    private BungeePool bungeePool;

    // Lobby server
    @YAMLComment("Pool of lobby servers")
    private ServerPool lobbyPool;

    // Servers, which have multiple replicates
    @YAMLComment("Multiple other server pools")
    private List<ServerPool> poolServers = new ArrayList<>();

    // Multiple servers, persistent
    @YAMLComment("Persistent server pools")
    private List<PersistentServerPool> persistentServerPool = new ArrayList<>();


    public InfrastructureConfig() {
        this(false);
    }

    public InfrastructureConfig(boolean init) {
        if (init) {
            this.nginxService = new NginxService("NginxProxy", "mcproxy", "512M", "1", 2, new ServiceConstraints(1), 25577);
            this.bungeePool = new BungeePool("BungeePool", "waterfall", "1G", "2", 2, new ServiceConstraints(1));
            this.lobbyPool = new ServerPool("LobbyServer", "testmc", "512M", "1", 2, null, 10);
            this.poolServers.add(
                    new ServerPool("Minigame-1", "testmc", "512M", "1", 2, null, 2)
            );
        }
    }

}
