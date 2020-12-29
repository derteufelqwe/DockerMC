package de.derteufelqwe.bungeeplugin.eventhandlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.istack.NotNull;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerJoinEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerLeaveEvent;
import de.derteufelqwe.bungeeplugin.redis.RedisDataCache;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerJoinNetwork;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerLeaveNetwork;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.MojangAPIProfile;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.MojangAPIProfileDeserializer;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.PlayerTextureDeserializer;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.PlayerLogin;
import lombok.NonNull;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.hibernate.Session;
import org.hibernate.Transaction;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.CheckForNull;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Dispatches BungeeCords default events to make sure all relevant DB entries exist and can be used by other connections
 */
public class EventsDispatcher implements Listener {

    private SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();
    private JedisPool jedisPool = BungeePlugin.getRedisHandler().getJedisPool();
    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();
    private Gson mojangGson = createMojangGson();


    public EventsDispatcher() {

    }

    // --- Player Join ---

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoinNetwork(LoginEvent event) {
        try {
            // ToDo: Maybe make these functions run in parallel
            this.prepareOnPlayerJoinNetworkDB(event);
            this.prepareOnPlayerJoinNetworkRedis(event);

            // --- Setup finished, call events ---

            this.callBungeePlayerJoinEvent(event);

        } catch (Exception e) {
            e.printStackTrace();
            event.setCancelled(true);
            event.setCancelReason(new TextComponent("Internal server error. Failed to login. Please notify the staff " +
                    "with the following timestamp: " + (System.currentTimeMillis() / 1000L)));
        }
    }

    /**
     * Creates or updates the relevant redis entries for the joining player
     * @param event
     */
    private void prepareOnPlayerJoinNetworkRedis(LoginEvent event) {
        InitialHandler handler = (InitialHandler) event.getConnection();
        String playerName = handler.getLoginRequest().getData();
        String uuid = handler.getUniqueId().toString();
        String userIp = handler.getAddress().toString().substring(1);
        RedisDataCache.PlayerData playerData = new RedisDataCache.PlayerData(playerName, uuid, userIp);

        // Add the relevant data to redis
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.incr("playerCount");
            jedis.incr("bungee#playerCount#" + BungeePlugin.BUNGEECORD_ID);
            jedis.sadd("bungee#players#" + BungeePlugin.BUNGEECORD_ID, playerName);
            jedis.hset("players#" + playerData.getUsername(), playerData.toMap());
        }
    }

    /**
     * Creates or updates the relevant DB objects for the joining player
     * @param event
     */
    private void prepareOnPlayerJoinNetworkDB(LoginEvent event) {
        InitialHandler handler = (InitialHandler) event.getConnection();
        String playerName = handler.getLoginRequest().getData();
        UUID uuid = handler.getUniqueId();

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                // Create player object when he joins
                DBPlayer dbPlayer = session.get(DBPlayer.class, uuid);
                if (dbPlayer == null) {
                    dbPlayer = new DBPlayer(uuid, playerName);
                }

                // Update players name if it changed
                if (!dbPlayer.getName().equals(playerName)) {
                    dbPlayer.setName(playerName);
                }

                // Update players texture
                if (dbPlayer.getLastSkinUpdate() == null ||
                        (System.currentTimeMillis() - dbPlayer.getLastSkinUpdate().getTime()) >= 10 * 60 * 1000) {  // 10 Minutes
                    MojangAPIProfile profileData = this.downloadPlayerProfileData(dbPlayer.getUuid());
                    dbPlayer.setLastSkinUpdate(new Timestamp(System.currentTimeMillis()));

                    if (profileData.getTexture() != null) {
                        MojangAPIProfile.PlayerTexture texture = profileData.getTexture();

                        BufferedImage skin = texture.downloadSkinImage();

                        if (skin != null) {
                            dbPlayer.setSkin(skin);
                        }
                    }
                }

                // Create a login object for the player
                PlayerLogin playerLogin = new PlayerLogin(dbPlayer);
                session.save(playerLogin);

                session.saveOrUpdate(dbPlayer);

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }

            tx.commit();
        }

    }

    /**
     * Calls the custom {@link BungeePlayerJoinEvent}
     * @param event
     */
    private void callBungeePlayerJoinEvent(LoginEvent event) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            InitialHandler handler = (InitialHandler) event.getConnection();
            String playerName = handler.getLoginRequest().getData();

            // Call the event locally
            BungeePlayerJoinEvent bungeeEvent = new BungeePlayerJoinEvent(playerName, new Callback<BungeePlayerJoinEvent>() {
                @Override
                public void done(BungeePlayerJoinEvent result, Throwable error) {

                }
            });
            bungeeEvent.callEvent();

            // Send redis message
            RedisPlayerJoinNetwork redisMessage = new RedisPlayerJoinNetwork(playerName);
            jedis.publish("messages#" + redisMessage.getMessageType(), redisMessage.serialize());
        }
    }

    // --- Player Leave ---

    @EventHandler
    public void onPlayerLeaveNetwork(PlayerDisconnectEvent event) {
        this.removePlayerFromRedis(event.getPlayer());
        this.finishPlayersDBEntries(event.getPlayer());

        // Setup finished - call events

        this.callBungeePlayerLeaveEvent(event.getPlayer());
    }

    /**
     * Removes player information from redis
     * @param player
     */
    private void removePlayerFromRedis(ProxiedPlayer player) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.decr("playerCount");
            jedis.decr("bungee#playerCount#" + BungeePlugin.BUNGEECORD_ID);
            jedis.srem("bungee#players#" + BungeePlugin.BUNGEECORD_ID, player.getDisplayName());
        }
    }

    /**
     * Adds data gathered on disconnect
     * @param player
     */
    private void finishPlayersDBEntries(ProxiedPlayer player) {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                // Create player object when he joins
                DBPlayer dbPlayer = session.get(DBPlayer.class, player.getUniqueId());

                dbPlayer.setLastOnline(new Timestamp(System.currentTimeMillis()));

                // Update the login object
                CriteriaBuilder cb = session.getCriteriaBuilder();
                CriteriaQuery<PlayerLogin> cq = cb.createQuery(PlayerLogin.class);
                Root<PlayerLogin> root = cq.from(PlayerLogin.class);

                cq.select(root)
                        .where(cb.equal(root.get("player"), dbPlayer))
                        .orderBy(cb.desc(root.get("joinTime")));

                Query query = session.createQuery(cq);
                query.setMaxResults(1);
                PlayerLogin playerLogin = (PlayerLogin) query.getSingleResult();

                playerLogin.setLeaveTime(new Timestamp(System.currentTimeMillis()));
                session.update(playerLogin);

                session.update(dbPlayer);

            } finally {
                tx.commit();
            }
        }
    }

    private void callBungeePlayerLeaveEvent(ProxiedPlayer player) {
        try (Jedis jedis = this.jedisPool.getResource()) {

            // Call the event locally
            BungeePlayerLeaveEvent bungeeEvent = new BungeePlayerLeaveEvent(player.getDisplayName(), new Callback<BungeePlayerLeaveEvent>() {
                @Override
                public void done(BungeePlayerLeaveEvent result, Throwable error) {

                }
            });
            bungeeEvent.callEvent();

            // Send redis message
            RedisPlayerLeaveNetwork redisMessage = new RedisPlayerLeaveNetwork(player.getDisplayName());
            jedis.publish("messages#" + redisMessage.getMessageType(), redisMessage.serialize());
        }
    }

    // --- Player connect to server ---

    @EventHandler
    public void onPlayerConnectToServer(ServerConnectedEvent event) {

    }

    // --- Player disconnect from server ---

    @EventHandler
    public void onPlayerDisconnectFromServer(ServerDisconnectEvent event) {

    }


    // -----  Utility methods  -----

    @NotNull
    private Gson createMojangGson() {
        return new GsonBuilder()
                .registerTypeAdapter(MojangAPIProfile.class, new MojangAPIProfileDeserializer())
                .registerTypeAdapter(MojangAPIProfile.PlayerTexture.class, new PlayerTextureDeserializer())
                .create();
    }

    @NotNull
    private MojangAPIProfile downloadPlayerProfileData(@NotNull UUID playerId) {
        String uid = playerId.toString().replace("-", "");

        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uid);
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            yc.getInputStream()));

            String response = "";
            String s;
            while ((s = in.readLine()) != null)
                response += s;
            in.close();

            // If no valid uuid was supplied return a default response
            if (response.equals("")) {
                return new MojangAPIProfile(playerId);
            }

            return mojangGson.fromJson(response, MojangAPIProfile.class);

        } catch (IOException e) {
            return new MojangAPIProfile(playerId);
        }
    }


}
