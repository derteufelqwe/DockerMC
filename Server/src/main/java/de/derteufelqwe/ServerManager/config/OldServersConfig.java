package de.derteufelqwe.ServerManager.config;

import de.derteufelqwe.ServerManager.setup.servers.PersistentServerPool;
import org.jetbrains.annotations.NotNull;
import de.derteufelqwe.ServerManager.config.objects.ServerPoolContainer;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.config.annotations.Comment;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds system critical data that shouldn't be modified by the user
 */
@Data
@NoArgsConstructor
public class OldServersConfig {

    @Nullable
    private BungeePool bungeePool;

    @Nullable
    private ServerPool lobbyPool;

    @NotNull
    private ServerPoolContainer poolServers = new ServerPoolContainer();

    @NotNull
    private List<PersistentServerPool> persistentServerPool = new ArrayList<>();

}
