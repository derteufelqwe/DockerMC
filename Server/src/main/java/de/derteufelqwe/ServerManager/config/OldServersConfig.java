package de.derteufelqwe.ServerManager.config;

import org.jetbrains.annotations.NotNull;
import de.derteufelqwe.ServerManager.config.objects.ServerPoolContainer;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.config.annotations.Comment;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

/**
 * Holds system critical data that shouldn't be modified by the user
 */
@Data
@NoArgsConstructor
public class OldServersConfig {

    @Comment("Last known config for the BungeePool")
    @Nullable
    private BungeePool bungeePool;

    @Comment("Last known config for the LobbyPool")
    @Nullable
    private ServerPool lobbyPool;

    @Comment("Last known config for the LobbyPool")
    @NotNull
    private ServerPoolContainer poolServers = new ServerPoolContainer();

}
