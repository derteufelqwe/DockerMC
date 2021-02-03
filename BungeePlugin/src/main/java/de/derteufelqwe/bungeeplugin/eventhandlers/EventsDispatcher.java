package de.derteufelqwe.bungeeplugin.eventhandlers;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerJoinEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerLeaveEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerServerChangeEvent;
import de.derteufelqwe.bungeeplugin.redis.PlayerData;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerJoinNetwork;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerLeaveNetwork;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerServerChange;
import de.derteufelqwe.bungeeplugin.runnables.DefaultCallback;
import de.derteufelqwe.bungeeplugin.runnables.PlayerSkinDownloadRunnable;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.exceptions.NotFoundException;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.*;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.hibernate.Session;
import org.hibernate.Transaction;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.*;

/**
 * Dispatches BungeeCords default events to make sure all relevant DB entries exist and can be used by other connections
 */
public class EventsDispatcher implements Listener {

    private SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();
    private JedisPool jedisPool = BungeePlugin.getRedisHandler().getJedisPool();
    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();

    /*
     * Contains players, joined the network but didn't connect to a server yet. This is used to identify when to send the
     * BungeePlayerServerChangeEvent.
     * If a players name is in here, the BungeePlayerServerChangeEvent gets sent in the ServerConnectEvent handler.
     * If its name is not in here the event will be sent in the ServerDisconnectEvent handler, because the player is
     * changing the server instead of newly connecting to one.
     */
    private Set<String> newlyJoinedPlayers = new HashSet<>();
    /*
     * If this map contains an entry for a player, this event will be sent when a player disconnects from a server.
     */
    private Map<String, RedisPlayerServerChange> playerServerChangeEventMap = new HashMap<>();


    public EventsDispatcher() {

    }

    // --- Player Join ---

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoinNetwork(LoginEvent event) {
        try {
            InitialHandler handler = (InitialHandler) event.getConnection();
            String playerName = handler.getLoginRequest().getData();
            this.newlyJoinedPlayers.add(playerName);

            // ToDo: Maybe make these functions run in parallel
            this.prepareOnPlayerJoinNetworkDB(event);
            if (this.checkPlayerBan(event))
                return;
            if (this.checkIPBan(event))
                return;

            this.prepareOnPlayerJoinNetworkRedis(event);

            // --- Setup finished, call events ---

            this.callBungeePlayerJoinEvent(event);

        } catch (Exception e) {
            e.printStackTrace();
            event.setCancelled(true);
            event.setCancelReason(new TextComponent(ChatColor.RED + "Internal server error. Failed to login. Please notify the staff " +
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
        String userIp = handler.getAddress().toString().substring(1).split(":")[0];
        PlayerData playerData = new PlayerData(playerName, uuid, userIp);

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
                    session.save(dbPlayer);
                }

                // Update players name if it changed
                if (!dbPlayer.getName().equals(playerName)) {
                    dbPlayer.setName(playerName);
                }

                // Create a login object for the player
                PlayerLogin playerLogin = new PlayerLogin(dbPlayer);
                session.save(playerLogin);

                session.saveOrUpdate(dbPlayer);
                tx.commit();

                // ToDo: Tidy this up. This is probably not the best Hibernate way

                tx = session.beginTransaction();

                // Update the players skin.
                // This is done asynchronously to prevent login times from up to 15 seconds.
                if (dbPlayer.getLastSkinUpdate() == null ||
                        (System.currentTimeMillis() - dbPlayer.getLastSkinUpdate().getTime()) >= 10 * 60 * 1000) { // 10 Minutes
                    ProxyServer.getInstance().getScheduler().runAsync(BungeePlugin.PLUGIN, new PlayerSkinDownloadRunnable(uuid));

                }

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }

            tx.commit();
        }

    }

    /**
     * Checks if a player is banned and prevents him from connecting if he is banned
     * @param event
     * @return Banned or not
     */
    private boolean checkPlayerBan(LoginEvent event) {
        InitialHandler handler = (InitialHandler) event.getConnection();
        UUID uuid = handler.getUniqueId();

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                // Create player object when he joins
                DBPlayer dbPlayer = session.get(DBPlayer.class, uuid);
                if (dbPlayer == null) {
                    return false;
                }

                PlayerBan activeBan = dbPlayer.getActiveBan();
                if (activeBan == null) {
                    return false;
                }

                event.setCancelled(true);
                if (activeBan.isPermanent()) {
                    event.setCancelReason(new TextComponent(ChatColor.RED + String.format(
                            "You are permanently banned. %s Reason: '%s'.", ChatColor.RESET, activeBan.getBanMessage()
                    )));

                } else {
                    event.setCancelReason(new TextComponent(ChatColor.RED + String.format(
                            "You are banned until %s. %s Reason: '%s'.",
                            Utils.formatTimestamp(activeBan.getBannedUntil()), ChatColor.RESET, activeBan.getBanMessage()
                    )));
                }


                tx.commit();

                return true;

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        }
    }

    /**
     * Checks if the users IP is banned
     * @param event
     * @return
     */
    private boolean checkIPBan(LoginEvent event) {
        InitialHandler handler = (InitialHandler) event.getConnection();
        String userIp = handler.getAddress().toString().substring(1).split(":")[0];

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                CriteriaBuilder cb = session.getCriteriaBuilder();
                CriteriaQuery<IPBan> cq = cb.createQuery(IPBan.class);
                Root<IPBan> root = cq.from(IPBan.class);

                cq.select(root).where(cb.equal(root.get("bannedIp"), userIp)).orderBy(cb.desc(root.get("bannedAt")));

                // --- Execute the query ---

                Query queryRes = session.createQuery(cq);
                List<IPBan> res = (List<IPBan>) queryRes.getResultList();

                IPBan ipBan = null;

                for (IPBan b : res) {
                    if (b.isActive()) {
                        ipBan = b;
                        break;
                    }
                }

                if (ipBan == null) {
                    return false;
                }

                event.setCancelled(true);
                event.setCancelReason(new TextComponent(ChatColor.RED + String.format(
                        "Your ip is banned until %s. %s Reason: '%s'.",
                        Utils.formatTimestamp(ipBan.getBannedUntil()), ChatColor.RESET, ipBan.getBanMessage()
                )));

                tx.commit();

                return true;

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }

        }

    }

    /**
     * Calls the custom {@link BungeePlayerJoinEvent}
     * @param event
     */
    private void callBungeePlayerJoinEvent(LoginEvent event) {
        InitialHandler handler = (InitialHandler) event.getConnection();
        String playerName = handler.getLoginRequest().getData();
        UUID uuid = handler.getUniqueId();

        // Call the event locally
        BungeePlayerJoinEvent bungeeEvent = new BungeePlayerJoinEvent(uuid, playerName, new DefaultCallback<>());
        bungeeEvent.callEvent();

        // Send redis message
        try (Jedis jedis = this.jedisPool.getResource()) {
            RedisPlayerJoinNetwork redisMessage = new RedisPlayerJoinNetwork(uuid, playerName);
            jedis.publish("messages#" + redisMessage.getMessageType(), redisMessage.serialize());
        }
    }

    // --- Player Leave ---

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeaveNetwork(PlayerDisconnectEvent event) {
        String playerName = event.getPlayer().getDisplayName();

        // Remove the player if he had no chance to connect to a server
        this.newlyJoinedPlayers.remove(playerName);
        this.playerServerChangeEventMap.remove(playerName);

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
            jedis.hdel("players#" + player.getDisplayName(), PlayerData.getFields());
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
            BungeePlayerLeaveEvent bungeeEvent = new BungeePlayerLeaveEvent(player.getUniqueId(), player.getDisplayName(), new DefaultCallback<>());
            bungeeEvent.callEvent();

            // Send redis message
            RedisPlayerLeaveNetwork redisMessage = new RedisPlayerLeaveNetwork(player.getUniqueId(), player.getDisplayName());
            jedis.publish("messages#" + redisMessage.getMessageType(), redisMessage.serialize());
        }
    }

    // --- Player connect to server ---

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerConnectToServer(ServerConnectedEvent event) {
        String serverName = event.getServer().getInfo().getName();
        String playerName = event.getPlayer().getDisplayName();

        this.updateRedisPlayerJoin(event.getPlayer(), serverName);

        // Prepare for the event call
        this.playerServerChangeEventMap.put(playerName, new RedisPlayerServerChange(playerName, serverName));

        if (this.newlyJoinedPlayers.contains(playerName)) {
            this.newlyJoinedPlayers.remove(playerName);
            this.callBungeePlayerServerChangeEvent(playerName);
        }
    }

    /**
     * Updates the information in redis about a players server, when he connects to a new server
     * @param player
     * @param serverName
     */
    private void updateRedisPlayerJoin(ProxiedPlayer player, String serverName) {
        String playerName = player.getDisplayName();

        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.incr("minecraft#playerCount#" + serverName);
            jedis.sadd("minecraft#players#" + serverName, playerName);
            jedis.hset("players#" + playerName, "server", serverName);
            jedis.set("playerJoinTime#" + playerName + "#" + serverName, Long.toString(System.currentTimeMillis() / 1000L));
        }

    }

    // --- Player disconnect from server ---

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDisconnectFromServer(ServerDisconnectEvent event) {
        String serverName = event.getTarget().getName();
        String playerName = event.getPlayer().getDisplayName();

        try (Jedis jedis = this.jedisPool.getResource()) {
            this.updateDBPlayerOnlineDuration(jedis, event.getPlayer(), serverName);
            this.updateRedisPlayerLeave(jedis, event.getPlayer(), serverName);
        }

        // This is only relevant when a player changes the server and not disconnects
        if (this.playerServerChangeEventMap.containsKey(playerName)) {
            this.playerServerChangeEventMap.get(playerName).setOldServer(serverName);
            this.callBungeePlayerServerChangeEvent(playerName);
        }

    }

    /**
     * Updates the players playtime on a service
     * @param jedis
     * @param player
     * @param serverName
     */
    private void updateDBPlayerOnlineDuration(Jedis jedis, ProxiedPlayer player, String serverName) {
        String rawServerName = serverName.split("-")[0];
        Long oldJoinTime = this.getOldJoinTime(jedis, player.getDisplayName(), serverName);

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBPlayer dbPlayer = session.get(DBPlayer.class, player.getUniqueId());
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

    /**
     * Updates redis to remove values that aren't required anymore
     * @param jedis
     * @param player
     * @param serverName
     */
    private void updateRedisPlayerLeave(Jedis jedis, ProxiedPlayer player, String serverName) {
        String playerName = player.getDisplayName();

        jedis.decr("minecraft#playerCount#" + serverName);
        jedis.srem("minecraft#players#" + serverName, playerName);
        jedis.del("playerJoinTime#" + playerName + "#" + serverName);

    }

    /**
     * Calls the event on all proxies
     * @param username
     */
    private void callBungeePlayerServerChangeEvent(String username) {
        RedisPlayerServerChange redisMessage = this.playerServerChangeEventMap.get(username);

        BungeePlayerServerChangeEvent event = new BungeePlayerServerChangeEvent(redisMessage, new Callback<BungeePlayerServerChangeEvent>() {
            @Override
            public void done(BungeePlayerServerChangeEvent result, Throwable error) {

            }
        });
        event.callEvent();

        // Send the redis message
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.publish("messages#" + redisMessage.getMessageType(), redisMessage.serialize());
        }

    }


    // -----  Utility methods  -----

    /**
     * Tries to parse the join time for a user on a server from redis
     * @param jedis
     * @param playerName
     * @param serverName
     * @return
     */
    private Long getOldJoinTime(Jedis jedis, String playerName, String serverName) throws NotFoundException {
        Long oldJoinTime = null;
        String oldJoinTimeString = jedis.get("playerJoinTime#" + playerName + "#" + serverName);

        if (oldJoinTimeString == null) {
            throw new NotFoundException("Jointime for player %s on server %s not found.", playerName, serverName);
        }

        try {
            return Long.parseLong(oldJoinTimeString);

        } catch (NumberFormatException e) {
            throw new NotFoundException("Jointime for player %s on server %s is %s and invalid.\n", playerName, serverName, oldJoinTimeString);
        }
    }

}
