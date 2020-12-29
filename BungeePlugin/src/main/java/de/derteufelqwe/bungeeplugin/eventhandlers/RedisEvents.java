package de.derteufelqwe.bungeeplugin.eventhandlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.istack.NotNull;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.TestEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerJoinEvent;
import de.derteufelqwe.bungeeplugin.redis.RedisDataCache;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.MojangAPIProfile;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.MojangAPIProfileDeserializer;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.PlayerTextureDeserializer;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.PlayerLogin;
import de.derteufelqwe.commons.hibernate.objects.PlayerOnlineDurations;
import lombok.SneakyThrows;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.hibernate.Session;
import org.hibernate.Transaction;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.CheckForNull;
import javax.persistence.NoResultException;
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
import java.util.concurrent.TimeUnit;


/**
 * Event order when changing servers: ServerConnectedEvent, ServerDisconnectEvent
 */
public class RedisEvents implements Listener {

    private SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();
    private JedisPool jedisPool = BungeePlugin.getRedisHandler().getJedisPool();
    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();
    private Gson mojangGson = createMojangGson();
    private int counter = 0;


    public RedisEvents() {
    }


    private Gson createMojangGson() {
        return new GsonBuilder()
                .registerTypeAdapter(MojangAPIProfile.class, new MojangAPIProfileDeserializer())
                .registerTypeAdapter(MojangAPIProfile.PlayerTexture.class, new PlayerTextureDeserializer())
                .create();
    }


    /**
     * Returns the server information on ping.
     */
    @EventHandler
    public void onProxyPingEvent(ProxyPingEvent event) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            String playerCountStr = jedis.get("playerCount");
            int playerCount = playerCountStr == null ? 0 : Integer.parseInt(playerCountStr);
            int playerLimit = ProxyServer.getInstance().getConfig().getPlayerLimit();
            playerLimit = playerLimit < 0 ? 65535 : playerLimit;
            event.getResponse().setPlayers(new ServerPing.Players(playerLimit, playerCount, null));
        }
    }

    /**
     * Adds a player to redis when he joins the network
     */
//    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinEvent(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        try (Jedis jedis = this.jedisPool.getResource()) {
            this.redisDataManager.addPlayer(new RedisDataCache.PlayerData(event.getPlayer()));

            jedis.incr("playerCount");
            jedis.incr("bungee#playerCount#" + BungeePlugin.BUNGEECORD_ID);
            jedis.sadd("bungee#players#" + BungeePlugin.BUNGEECORD_ID, event.getPlayer().getDisplayName());
        }

        // Create player object
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                // Create player object when he joins
                DBPlayer dbPlayer = session.get(DBPlayer.class, player.getUniqueId());
                if (dbPlayer == null) {
                    dbPlayer = new DBPlayer(player.getUniqueId(), player.getDisplayName());
                }


                // Update players name if it changed
                if (!dbPlayer.getName().equals(player.getDisplayName())) {
                    dbPlayer.setName(player.getDisplayName());
                }


                // Update players texture
                if (dbPlayer.getLastSkinUpdate() == null || (System.currentTimeMillis() - dbPlayer.getLastSkinUpdate().getTime()) >= 10 * 60 * 1000) {  // 10 Minutes
                    MojangAPIProfile profileData = this.downloadPlayerProfileData(dbPlayer.getUuid());
                    dbPlayer.setLastSkinUpdate(new Timestamp(System.currentTimeMillis()));

                    if (profileData != null && profileData.getTexture() != null) {
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

            } finally {
                tx.commit();
            }

        }
    }

    /**
     * Removes a player from redis when he disconnects from the network
     */
//    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        // Redis stuff
        try (Jedis jedis = this.jedisPool.getResource()) {
            this.redisDataManager.removePlayer(event.getPlayer().getDisplayName());

            jedis.decr("playerCount");
            jedis.decr("bungee#playerCount#" + BungeePlugin.BUNGEECORD_ID);
            jedis.srem("bungee#players#" + BungeePlugin.BUNGEECORD_ID, event.getPlayer().getDisplayName());
        }

        // Create player object
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

    /**
     * Changes the server information for the player, when he changes the server.
     * Increments player counts on a server
     */
    @SneakyThrows
//    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerServerConnect(ServerConnectedEvent event) {
        String serverName = event.getServer().getInfo().getName();
        String playerName = event.getPlayer().getDisplayName();
        this.redisDataManager.updatePlayersServer(playerName, serverName);

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.incr("minecraft#playerCount#" + serverName);
            jedis.sadd("minecraft#players#" + serverName, playerName);
            jedis.set("players#joinTime#" + playerName + "#" + serverName, Long.toString(System.currentTimeMillis() / 1000L));
        }


    }

    /**
     * Redis handler when players disconnec from a server
     */
//    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerServerDisconnect(ServerDisconnectEvent event) {
        String serverName = event.getTarget().getName();
        String playerName = event.getPlayer().getDisplayName();
        String rawServerName = serverName.split("-")[0];
        Long oldJoinTime = null;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.decr("minecraft#playerCount#" + serverName);
            jedis.srem("minecraft#players#" + serverName, playerName);
            oldJoinTime = Long.parseLong(jedis.get("players#joinTime#" + playerName + "#" + serverName));
            jedis.del("players#joinTime#" + playerName + "#" + serverName);
        }

        // Update the playtime in the db
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBPlayer dbPlayer = session.get(DBPlayer.class, event.getPlayer().getUniqueId());
                DBService dbService = null;
                PlayerOnlineDurations playerOnlineDurations;
                CriteriaBuilder cb = session.getCriteriaBuilder();

                // Try to get the DBService
                {
                    CriteriaQuery<DBService> cq = cb.createQuery(DBService.class);
                    Root<DBService> serviceRoot = cq.from(DBService.class);

                    cq.select(serviceRoot)
                            .where(cb.equal(serviceRoot.get("name"), rawServerName))
                            .orderBy(cb.desc(serviceRoot.get("id")))
                    ;

                    Query queryRes = session.createQuery(cq);
                    queryRes.setMaxResults(1);

                    dbService = (DBService) queryRes.getSingleResult();
                }


                // Try to get PlayerOnlineDurations object
                {
                    CriteriaQuery<PlayerOnlineDurations> cq = cb.createQuery(PlayerOnlineDurations.class);
                    Root<PlayerOnlineDurations> onlineRoot = cq.from(PlayerOnlineDurations.class);

                    cq.select(onlineRoot)
                            .where(cb.equal(onlineRoot.get("player"), dbPlayer), cb.equal(onlineRoot.get("service"), dbService))
                    ;

                    Query queryres = session.createQuery(cq);
                    queryres.setMaxResults(1);

                    try {
                        playerOnlineDurations = (PlayerOnlineDurations) queryres.getSingleResult();

                    } catch (NoResultException e) {
                        playerOnlineDurations = new PlayerOnlineDurations(dbPlayer, dbService);
                        session.persist(playerOnlineDurations);
                    }
                }


                int onlineTime = (int) ((System.currentTimeMillis() / 1000L) - oldJoinTime);
                playerOnlineDurations.setDuration(playerOnlineDurations.getDuration() + onlineTime);

                session.update(playerOnlineDurations);

            } finally {
                tx.commit();
            }

        }
    }

    // -----  Utility methods  -----

    @CheckForNull
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

        } catch (IOException e1) {
            return null;
        }
    }

    public void add() {

    }

//    @EventHandler
    @SneakyThrows
    public void ononLogin(LoginEvent event) {
//        event.registerIntent(BungeePlugin.PLUGIN);
//        add();
//        ProxyServer.getInstance().getScheduler().runAsync(BungeePlugin.PLUGIN, () -> {
//            try {
//                TimeUnit.SECONDS.sleep(3);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println("call2 done.");
//            event.completeIntent(BungeePlugin.PLUGIN);
//        });
//

        System.out.println("handler2 done");
    }

//    @EventHandler
    @SneakyThrows
    public void onLogin(ServerConnectedEvent event) {

        TestEvent testEvent = new TestEvent(event.getPlayer(), new Callback<TestEvent>() {
            @Override
            public void done(TestEvent result, Throwable error) {
                System.out.println("callback done");
                result.completeIntent(BungeePlugin.PLUGIN);
            }
        });

        testEvent.registerIntent(BungeePlugin.PLUGIN);
        testEvent.callEvent();

    }


//    @EventHandler
    @SneakyThrows
    public void onTest(TestEvent event) {

        TimeUnit.SECONDS.sleep(3);

//        event.registerIntent(BungeePlugin.PLUGIN);
//
//        ProxyServer.getInstance().getScheduler().runAsync(BungeePlugin.PLUGIN, () -> {
//            try {
//                TimeUnit.SECONDS.sleep(3);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println("call done.");
//            event.completeIntent(BungeePlugin.PLUGIN);
//        });
//
//        System.out.println("handler done");
    }

}
