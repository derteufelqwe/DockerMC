package de.derteufelqwe.ServerManager.config;

import com.sun.istack.internal.NotNull;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.config.annotations.Comment;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.C;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds system critical data that shouldn't be modified by the user
 */
@Data
@NoArgsConstructor
public class SystemConfig {

    @Comment("Last known config for the BungeePool")
    @Nullable private BungeePool bungeePool;

    @Comment("Last known config for the LobbyPool")
    @Nullable private ServerPool lobbyPool;

    @Comment("Last known config for the LobbyPool")
    @NotNull private List<ServerPool> poolServers = new ArrayList<>();

}
