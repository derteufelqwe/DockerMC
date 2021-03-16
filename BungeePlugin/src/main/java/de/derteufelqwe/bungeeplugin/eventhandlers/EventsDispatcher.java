package de.derteufelqwe.bungeeplugin.eventhandlers;

import org.jetbrains.annotations.Nullable;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerJoinEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerLeaveEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerServerChangeEvent;
import de.derteufelqwe.bungeeplugin.redis.PlayerData;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.bungeeplugin.runnables.DefaultCallback;
import de.derteufelqwe.bungeeplugin.runnables.PlayerSkinDownloadRunnable;
import de.derteufelqwe.bungeeplugin.runnables.WaitRunnable;
import de.derteufelqwe.bungeeplugin.utils.ServerInfoStorage;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.exceptions.InvalidStateError;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.*;
import de.derteufelqwe.commons.logger.DMCLogger;
import de.derteufelqwe.commons.protobuf.RedisMessages;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.protocol.packet.LoginRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.*;

/**
 * Dispatches BungeeCords default events to make sure all relevant DB entries exist and can be used by other connections
 */
public class EventsDispatcher implements Listener {

    /*
     * Event order:
     * Connecting:    ServerConnectEvent -> ServerSwitchEvent
     * Changing:      ServerConnectEvent -> ServerSwitchEvent -> ServerDisconnectEvent
     * Disconnecting: ServerDisconnectEvent
     */

    /*
     * Maybe improve login scheduler performance:
     *     Depending on your use case, you can group your work into 1 async task per player,
     *     and you can use a thread pool with a different thread management policy
     */

    private SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();
    private JedisPool jedisPool = BungeePlugin.getRedisPool().getJedisPool();
    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();
    private DMCLogger logger = BungeePlugin.getDmcLogger();
    private ServerInfoStorage serverInfoStorage = BungeePlugin.getServerInfoStorage();
    private TaskScheduler scheduler = ProxyServer.getInstance().getScheduler();

    private TextComponent errorMessage = new TextComponent(ChatColor.RED + String.format("Internal server error. Failed to login. " +
            "Please notify the staff with id '%s'! Retry in a few seconds.", BungeePlugin.META_DATA.readContainerID()));

    private RedisMessages.BungeeMessageBase messageBase;
    private final Method getLoginRequestMethod;


    public EventsDispatcher() {
        this.messageBase = RedisMessages.BungeeMessageBase.newBuilder()
                .setBungeeCordId(BungeePlugin.BUNGEECORD_ID)
                .build();

        // Get the method from a user connection, that gives access to the username
        try {
            getLoginRequestMethod = Class.forName("net.md_5.bungee.connection.InitialHandler").getMethod("getLoginRequest");

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Couldn't find method 'getLoginRequest' on class 'InitialHandler'.", e);
        }
    }

    // --- Player Join ---

    /**
     * Handles a player joining the network. This involves the following steps:
     *  1. Create / update the DB entry for the player
     *  2. Check if the player or his ip is banned
     *  3. Add the joined player to redis
     *  4. Call the BungeePlayerJoinEvent
     */
    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoinNetwork(LoginEvent event) {
        try {
            final String playerName = this.extractUsernameFromConnection(event.getConnection());
            final UUID playerId = event.getConnection().getUniqueId();
            final String connectionIp = event.getConnection().getSocketAddress().toString().substring(1).split(":")[0];

            long allStart = System.currentTimeMillis();

            WaitRunnable<Void> task1 = (WaitRunnable<Void>) scheduler.runAsync(BungeePlugin.PLUGIN, new WaitRunnable<Void>() {
                @Override
                public Void exec() {
                    long start = System.currentTimeMillis();
                    addPlayerToDB(playerId, playerName);
                    logger.finer("prepareOnPlayerJoin took %s ms.", System.currentTimeMillis() - start);
                    return null;
                }
            }).getTask();

            WaitRunnable<Boolean> task2 = (WaitRunnable<Boolean>) scheduler.runAsync(BungeePlugin.PLUGIN, new WaitRunnable<Boolean>() {
                @Override
                public Boolean exec() {
                    long start = System.currentTimeMillis();
                    boolean isBanned = checkPlayerBan(event, playerId);
                    logger.finer("checkPlayerBan took %s ms.", System.currentTimeMillis() - start);
                    return isBanned;
                }
            }).getTask();

            WaitRunnable<Boolean> task3 = (WaitRunnable<Boolean>) scheduler.runAsync(BungeePlugin.PLUGIN, new WaitRunnable<Boolean>() {
                @Override
                public Boolean exec() {
                    long start = System.currentTimeMillis();
                    boolean isBanned = checkIPBan(event, connectionIp);
                    logger.finer("checkPlayerBan took %s ms.", System.currentTimeMillis() - start);
                    return isBanned;
                }
            }).getTask();

            boolean finished1 = task1.awaitCompletion(2000);
            boolean finished2 = task2.awaitCompletion(2000);
            boolean finished3 = task3.awaitCompletion(2000);

            logger.finer("Overall login duration: " + (System.currentTimeMillis() - allStart) + " ms.");

            // Don't allow a login when not all of the tasks finished.
            if (!finished1 || !finished2 || !finished3) {
                logger.warning("Player %s failed to login. An async task took too long.", playerName);
                event.setCancelled(true);
                event.setCancelReason(errorMessage);
                return;
            }

            // Don't allow banned players to join.
            if (task2.getResult() != null && task2.getResult().equals(true)) {
                return;
            } else if (task3.getResult() != null && task3.getResult().equals(true)) {
                return;
            }

            this.addJoinedPlayerToRedis(playerName, playerId, connectionIp);

            // --- Setup finished, call events ---

            this.callBungeePlayerJoinEvent(playerName, playerId);

        } catch (Exception e) {
            e.printStackTrace();
            event.setCancelled(true);
            event.setCancelReason(errorMessage);
        }
    }

    /**
     * Creates or updates the relevant redis entries for the joining player
     */
    private void addJoinedPlayerToRedis(String username, UUID playerId, String connectionIp) {
        PlayerData playerData = new PlayerData(username, playerId.toString(), connectionIp);

        // Add the relevant data to redis
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.incr("playerCount");
            jedis.incr("bungee#playerCount#" + BungeePlugin.BUNGEECORD_ID);
            jedis.sadd("bungee#players#" + BungeePlugin.BUNGEECORD_ID, username);
            jedis.hset("players#" + playerData.getUsername(), playerData.toMap());
        }
    }

    /**
     * Creates or updates the relevant DB objects for the joining player and downloads their skin.
     */
    private void addPlayerToDB(UUID playerId, String username) {
        DBPlayer dbPlayer;

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                // Create player object when he joins
                dbPlayer = session.get(DBPlayer.class, playerId);
                if (dbPlayer == null) {
                    dbPlayer = new DBPlayer(playerId, username);
                    session.save(dbPlayer);
                }

                // ToDo: change other players name to support name changes

                // Update players name if it changed
                if (!dbPlayer.getName().equals(username)) {
                    dbPlayer.setName(username);
                }

                session.saveOrUpdate(dbPlayer);
                tx.commit();

                // ToDo: Tidy this up. This is probably not the best Hibernate way

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        }


        if (dbPlayer != null) {
            // Update the players skin.
            // This is done asynchronously to prevent login times from up to 15 seconds.
            if (dbPlayer.getLastSkinUpdate() == null ||
                    (System.currentTimeMillis() - dbPlayer.getLastSkinUpdate().getTime()) >= 10 * 60 * 1000) { // 10 Minutes
                ProxyServer.getInstance().getScheduler().runAsync(BungeePlugin.PLUGIN, new PlayerSkinDownloadRunnable(playerId));

            }
        }
    }

    /**
     * Checks if a player is banned and prevents him from connecting if he is banned
     * @return true = banned
     */
    private boolean checkPlayerBan(LoginEvent event, UUID playerId) {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                // Create player object when he joins
                DBPlayer dbPlayer = session.get(DBPlayer.class, playerId);
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
     * @return true = banned
     */
    private boolean checkIPBan(LoginEvent event, String userIp) {
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
     * Calls the custom {@link BungeePlayerJoinEvent}. This calls the BungeeCord event locally and the redis message
     */
    private void callBungeePlayerJoinEvent(String username, UUID playerId) {

        // Call the event locally
        BungeePlayerJoinEvent bungeeEvent = new BungeePlayerJoinEvent(playerId, username, new DefaultCallback<>());
        bungeeEvent.callEvent();

        RedisMessages.PlayerJoinNetwork playerJoinNetwork = RedisMessages.PlayerJoinNetwork.newBuilder()
                .setBase(this.messageBase)
                .setUuid(this.getUUID(playerId))
                .setUsername(username)
                .build();

        // Send redis message
        redisDataManager.sendMessage(playerJoinNetwork);
    }

    // --- Player Leave ---

    /**
     * Handles players leaving the network. This involves the following steps:
     *  1. Remove the player from redis
     *  2. Call the BungeePlayerLeaveEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeaveNetwork(PlayerDisconnectEvent event) {
        this.removePlayerFromRedis(event.getPlayer());

        this.finishLoginDBEntries(event.getPlayer());

        // Setup finished - call events
        this.callBungeePlayerLeaveEvent(event.getPlayer());
    }

    /**
     * Removes player information from redis
     *
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

    private void callBungeePlayerLeaveEvent(ProxiedPlayer player) {
        try (Jedis jedis = this.jedisPool.getResource()) {

            // Call the event locally
            BungeePlayerLeaveEvent bungeeEvent = new BungeePlayerLeaveEvent(player.getUniqueId(), player.getDisplayName(), new DefaultCallback<>());
            bungeeEvent.callEvent();

            RedisMessages.PlayerLeaveNetwork playerLeaveNetwork = RedisMessages.PlayerLeaveNetwork.newBuilder()
                    .setBase(this.messageBase)
                    .setUuid(this.getUUID(player.getUniqueId()))
                    .setUsername(player.getDisplayName())
                    .build();

            // Send redis message
            redisDataManager.sendMessage(playerLeaveNetwork);
        }
    }

    // --- Player connect to server ---

    /**
     * Handles a player connecting to a Minecraft server. This involves the following steps:
     *  1. Update the server entries / players server in redis
     *  2. Create the PlayerLogin entry in the DB and finish the old entry
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerConnectToServer(ServerConnectedEvent event) {
        String serverName = event.getServer().getInfo().getName();

        this.updateRedisPlayerJoin(event.getPlayer(), serverName);
        this.finishLoginDBEntries(event.getPlayer());
        this.createLoginDBEntry(event.getPlayer(), event.getServer().getInfo());
    }

    /**
     * Updates the information in redis about a players server, when he connects to a new server
     *
     * @param player
     * @param serverName
     */
    private void updateRedisPlayerJoin(ProxiedPlayer player, String serverName) {
        String playerName = player.getDisplayName();

        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.incr("minecraft#playerCount#" + serverName);
            jedis.sadd("minecraft#players#" + serverName, playerName);
            jedis.hset("players#" + playerName, "server", serverName);
        }

    }

    private void finishLoginDBEntries(ProxiedPlayer player) {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                List<PlayerLogin> logins = session.createNativeQuery(
                        "SELECT * FROM player_logins AS pl WHERE pl.player_uuid = :playerid AND pl.leavetime IS NULL",
                        PlayerLogin.class)
                        .setParameter("playerid", player.getUniqueId())
                        .getResultList();

                for (PlayerLogin login : logins) {
                    login.setLeaveTime(new Timestamp(System.currentTimeMillis()));
                    session.persist(login);
                }

            } catch (Exception e) {
                tx.rollback();
                throw e;

            } finally {
                tx.commit();
            }
        }
    }

    private void createLoginDBEntry(ProxiedPlayer player, ServerInfo server) {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBPlayer dbPlayer = session.get(DBPlayer.class, player.getUniqueId());
                DBService dbService = session.getReference(DBService.class, this.serverInfoStorage.get(server.getName()).getServiceId());

                PlayerLogin newLogin = new PlayerLogin(dbPlayer, dbService);
                session.persist(newLogin);

            } catch (Exception e) {
                tx.rollback();
                throw e;

            } finally {
                tx.commit();
            }
        }
    }

    // --- Player disconnect from server ---

    /**
     * Handles a player disconnecting from a Minecraft server. This involves the following steps:
     *  1. Update the server entries / players server in redis
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDisconnectFromServer(ServerDisconnectEvent event) {
        String serverName = event.getTarget().getName();

        try (Jedis jedis = this.jedisPool.getResource()) {
            this.updateRedisPlayerLeave(jedis, event.getPlayer(), serverName);
        }
    }

    /**
     * Updates redis to remove values that aren't required anymore
     *
     * @param jedis
     * @param player
     * @param serverName
     */
    private void updateRedisPlayerLeave(Jedis jedis, ProxiedPlayer player, String serverName) {
        String playerName = player.getDisplayName();

        jedis.decr("minecraft#playerCount#" + serverName);
        jedis.srem("minecraft#players#" + serverName, playerName);
    }

    /**
     * Informs all BungeeCord instances that a player changed a server
     */
    private void callBungeePlayerServerChangeEvent(ProxiedPlayer player, @Nullable ServerInfo from, ServerInfo to) {
        String oldServerName = from == null ? null : from.getName();

        BungeePlayerServerChangeEvent event = new BungeePlayerServerChangeEvent(
                player.getUniqueId(), player.getName(), oldServerName, to.getName(), new DefaultCallback<>()
        );
        event.callEvent();

        String oldServerNameNotNull = oldServerName == null ? "" : oldServerName;

        RedisMessages.PlayerChangeServer changeServerBuilder = RedisMessages.PlayerChangeServer.newBuilder()
                .setBase(this.messageBase)
                .setUuid(this.getUUID(player.getUniqueId()))
                .setUsername(player.getName())
                .setNewServer(oldServerNameNotNull)
                .setOldServer(to.getName())
                .build();

        // Send the redis message
        redisDataManager.sendMessage(changeServerBuilder);
    }

    // --- Player change server ---

    /**
     * Handles a player changing a Minecraft server. This involves the following steps:
     *  1. Call the BungeePlayerServerChangeEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChangeServer(ServerSwitchEvent event) {
        ServerInfo oldServer = event.getFrom();
        ServerInfo newServer = event.getPlayer().getServer().getInfo();

        this.callBungeePlayerServerChangeEvent(event.getPlayer(), oldServer, newServer);
    }


    // -----  Utility methods  -----

    /**
     * Converts a java UUID to Protobuf UUID
     *
     * @param uuid
     * @return
     */
    private RedisMessages.UUID getUUID(UUID uuid) {
        return RedisMessages.UUID.newBuilder()
                .setData(uuid.toString())
                .build();
    }

    private String extractUsernameFromConnection(PendingConnection connection) {
        try {
            return ((LoginRequest) getLoginRequestMethod.invoke(connection)).getData();

        } catch (ReflectiveOperationException e) {
            e.printStackTrace(System.err);
            throw new InvalidStateError("Couldn't extract username from connection.");
        }
    }

}
