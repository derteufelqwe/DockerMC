package de.derteufelqwe.bungeeplugin.redis;

import com.sun.istack.NotNull;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains all relevant information of a player.
 * Also represents the player information that are stored in redis
 */
@Getter
public class PlayerData {

    private String username;
    private String uuid;
    private String address;
    @Setter
    private String server;
    @Setter
    private String bungeeCordId = BungeePlugin.BUNGEECORD_ID;

    public PlayerData(@NotNull Map<String, String> input) {
        this.username = input.get("username");
        this.uuid = input.get("uuid");
        this.address = input.get("address");
        this.server = input.get("server");
        this.bungeeCordId = input.get("bungeeCordId");
    }

    public PlayerData(@NotNull ProxiedPlayer player) {
        this.username = player.getDisplayName();
        this.uuid = player.getUniqueId().toString();
        this.address = player.getAddress().toString();

        Server server = player.getServer();
        if (server == null)
            return;
        ServerInfo serverInfo = server.getInfo();
        if (serverInfo == null)
            return;
        this.server = serverInfo.getName();
    }

    public PlayerData(String username, String uuid, String address) {
        this.username = username;
        this.uuid = uuid;
        this.address = address;
    }


    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();

        map.put("username", this.username);
        map.put("uuid", this.uuid);
        map.put("address", this.address);
        if (this.server != null)
            map.put("server", this.server);
        if (this.bungeeCordId != null)
            map.put("bungeeCordId", this.bungeeCordId);

        return map;
    }

    /**
     * Returns all class fields, which are saved in redis.
     */
    public static String[] getFields() {
        return new String[]{"username", "uuid", "address", "server", "bungeeCordId"};
    }

    /**
     * Returns the name of the service the player is connected to
     * @return
     */
    @NotNull
    public String getServiceName() {
        return this.server.split("-")[0];
    }

}
