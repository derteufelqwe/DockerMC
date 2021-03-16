package de.derteufelqwe.ServerManager.config;

import org.jetbrains.annotations.NotNull;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.PersistentServerPool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.commons.config.annotations.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServersConfig {

    // BungeeCord servers
    @Comment("BungeeCord servers")
    @Nullable
    private BungeePool bungeePool;

    // Lobby server
    @Comment("Pool of lobby servers")
    @Nullable
    private ServerPool lobbyPool;

    // Servers, which have multiple replicates
    @Comment("Multiple other server pools")
    @NotNull
    private List<ServerPool> poolServers = new ArrayList<>();

    // Multiple servers, persistent
    @Comment("Persistent server pools")
    @NotNull
    private List<PersistentServerPool> persistentServerPool = new ArrayList<>();

}
