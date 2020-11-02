package de.derteufelqwe.ServerManager.config;

import com.sun.istack.internal.NotNull;
import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.infrastructure.NginxService;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.PersistentServerPool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.config.annotations.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class InfrastructureConfig {

    // Nginx server
    @Comment("Nginx reverse proxy")
    @Deprecated
    @Nullable private NginxService nginxService;

    // BungeeCord servers
    @Comment("BungeeCord servers")
    @Nullable private BungeePool bungeePool;

    // Lobby server
    @Comment("Pool of lobby servers")
    @Nullable private ServerPool lobbyPool;

    // Servers, which have multiple replicates
    @Comment("Multiple other server pools")
    @NotNull private List<ServerPool> poolServers = new ArrayList<>();

    // Multiple servers, persistent
    @Comment("Persistent server pools")
    @NotNull private List<PersistentServerPool> persistentServerPool = new ArrayList<>();


    public InfrastructureConfig() {
    }

    /**
     * Creates an example config
     * @return
     */
    public static InfrastructureConfig example() {
        InfrastructureConfig config = new InfrastructureConfig();

        config.setBungeePool(new BungeePool("BungeePool", "waterfall", "1G", 1.0F, 2, new ServiceConstraints(1), 25577));
        config.setLobbyPool(new ServerPool("LobbyServer", "testmc", "512M", 1.0F, 2, null, 10));
        config.getPoolServers().add(new ServerPool("Minigame-1", "testmc", "512M", 1.0F, 2, null, 2));

        return config;
    }

}
