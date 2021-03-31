package de.derteufelqwe.bungeeplugin;

import co.aikar.commands.BungeeCommandManager;
import de.derteufelqwe.bungeeplugin.api.BungeeAPI;
import de.derteufelqwe.bungeeplugin.commands.DMCInfo;
import de.derteufelqwe.bungeeplugin.commands.DebugCmd;
import de.derteufelqwe.bungeeplugin.commands.TestCmd;
import de.derteufelqwe.bungeeplugin.commands.misc.*;
import de.derteufelqwe.bungeeplugin.commands.permission.PermissionCommand;
import de.derteufelqwe.bungeeplugin.commands.permission.PermissionGroupCommand;
import de.derteufelqwe.bungeeplugin.eventhandlers.*;
import de.derteufelqwe.bungeeplugin.events.DMCServerAddEvent;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.bungeeplugin.redis.RedisPublishListener;
import de.derteufelqwe.bungeeplugin.runnables.DefaultCallback;
import de.derteufelqwe.bungeeplugin.utils.MetaData;
import de.derteufelqwe.bungeeplugin.utils.ServerInfoStorage;
import de.derteufelqwe.bungeeplugin.utils.ServerState;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import de.derteufelqwe.commons.health.HealthCheck;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.logger.DMCLogger;
import de.derteufelqwe.commons.redis.RedisPool;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.hibernate.Session;
import org.hibernate.Transaction;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BungeePlugin extends Plugin {

    // --- Infrastructure ---
    private final HealthCheck healthCheck = new HealthCheck();
    @Getter
    public static RedisPool redisPool = new RedisPool("redis");
    @Getter
    public static SessionBuilder sessionBuilder = new SessionBuilder("dockermc", "admin", "ubuntu1", Constants.POSTGRESDB_PORT);
    @Getter
    private static RedisDataManager redisDataManager;   // Manage data from and to redis
    private final RedisPublishListener redisPublishListener = new RedisPublishListener();

    // --- Static ---
    public static Plugin PLUGIN;
    public static ServerState STATE = ServerState.STARTING;
    @Getter
    private static Config<ConfigFile> CONFIG = new Config<>(new DefaultYamlConverter(), new DefaultGsonProvider(), "plugins/BungeePlugin/config.yml", new ConfigFile());

    public static final MetaData META_DATA = new MetaData();
    public static final String BUNGEECORD_ID = META_DATA.getTaskName(); // Identifies the current node
    @Getter
    private static final BungeeAPI bungeeApi = new BungeeAPI();
    @Getter
    private static DMCLogger dmcLogger;
    @Getter
    private static final ServerInfoStorage serverInfoStorage = new ServerInfoStorage();

    private final BungeeCommandManager commandManager = new BungeeCommandManager(this);


    @Override
    public void onEnable() {
        System.out.println("### Enabling DockerMC BungeeCord plugin ###we");

        // --- Setup ---
        this.addSignalHandlers();
        BungeePlugin.PLUGIN = this;
        CONFIG.load();

        // --- Logger ---
        dmcLogger = new DMCLogger("DMCLogger", Level.WARNING, getLogger());
        dmcLogger.parseLevel(CONFIG.get().getLogLevel());

        // --- Redis stuff ---
        BungeePlugin.redisDataManager = new RedisDataManager();
        BungeePlugin.redisDataManager.init();

        dmcLogger.info("Starting redis listener thread...");
        ProxyServer.getInstance().getScheduler().runAsync(BungeePlugin.PLUGIN, this.redisPublishListener);

        // ---  Events  ---
        getProxy().getPluginManager().registerListener(this, new ConnectionEvents());
        getProxy().getPluginManager().registerListener(this, new GeneralEvents());
        getProxy().getPluginManager().registerListener(this, new EventsDispatcher());
        getProxy().getPluginManager().registerListener(this, new BungeeEventsHandler());
        getProxy().getPluginManager().registerListener(this, new PermissionEvent());

        // ---  Commands  ---
        getProxy().getPluginManager().registerCommand(this, new DockerMCCommand());
        getProxy().getPluginManager().registerCommand(this, new FindCommand());
        getProxy().getPluginManager().registerCommand(this, new GlistCommand());
        getProxy().getPluginManager().registerCommand(this, new SendCommand());
        getProxy().getPluginManager().registerCommand(this, new BlistCommand());
        getProxy().getPluginManager().registerCommand(this, new KickCommand());
        getProxy().getPluginManager().registerCommand(this, new BanCommand());
        getProxy().getPluginManager().registerCommand(this, new UnbanCommand());
        getProxy().getPluginManager().registerCommand(this, new PlayerStatsCommand());
        getProxy().getPluginManager().registerCommand(this, new TestCmd());
        commandManager.registerCommand(new PermissionCommand());
        commandManager.registerCommand(new PermissionGroupCommand());
        commandManager.registerCommand(new DebugCmd());
        commandManager.registerCommand(new DMCInfo());


        // --- Misc ---
        this.setupConsoleUserInDatabase();
        this.healthCheck.start();

        // --- Requires previous initializations ---
        this.loadRunningServersFromDatabase();
        this.registerContainer();

        BungeePlugin.STATE = ServerState.RUNNING;
        dmcLogger.info("[System] Server %s (%s) started successfully.\n", META_DATA.getTaskName(), META_DATA.readContainerID());
    }

    @Override
    public void onDisable() {
        BungeePlugin.STATE = ServerState.STOPPING;
        System.out.printf("[System] Stopping Server %s.\n", BUNGEECORD_ID);

        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            player.disconnect(new TextComponent(ChatColor.RED + "BungeeCord Proxy shutting down!"));
        }

        this.deregisterContainer();

        // --- Stop all connections ---
        this.healthCheck.stop();
        BungeePlugin.redisPool.destroy();
    }

    /**
     * Registers handlers for UNIX signals to ensure correct server shutdown, when the server gets forcefully stopped.
     */
    private void addSignalHandlers() {
        SignalHandler signalHandler = new SignalHandler() {
            @Override
            public void handle(Signal signal) {
                System.err.println("HANDLING SIGNAL " + signal);
                ProxyServer.getInstance().stop("Received " + signal + " signal.");
            }
        };

        Signal.handle(new Signal("TERM"), signalHandler);
        Signal.handle(new Signal("INT"), signalHandler);
        Signal.handle(new Signal("HUP"), signalHandler);
    }

    /**
     * Creates the Console User in the database
     */
    private void setupConsoleUserInDatabase() {

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            DBPlayer player = session.get(DBPlayer.class, Constants.CONSOLE_USER_UUID);
            if (player == null) {
                System.out.println("Created console user");
                player = new DBPlayer(Constants.CONSOLE_USER_UUID, Constants.CONSOLE_USER_NAME);
                session.persist(player);
            }

            tx.commit();
        }

    }

    /**
     * Loads all running servers from the database and issues a DMCServerAddEvent for them
     */
    private void loadRunningServersFromDatabase() {
        try (Session session = sessionBuilder.openSession()) {
            List<DBContainer> containers = CommonsAPI.getInstance().getRunningMinecraftContainersFromDB(session);

            for (DBContainer container : containers) {
                try {
                    DMCServerAddEvent addServerEvent = new DMCServerAddEvent(
                            container.getMinecraftServerName(), (Inet4Address) Inet4Address.getByName(container.getIp()),
                            container.getId(), container.getService().getId(), new DefaultCallback<>()
                    );
                    addServerEvent.callEvent();

                } catch (UnknownHostException e) {
                    System.err.printf("Failed to add server '%s' with invalid ip '%s'.\n", container.getId(), container.getIp());
                }
            }
        }
    }

    /**
     * Register this container to Consul
     */
    private void registerContainer() {

    }

    /**
     * Remove this container from Consul
     */
    private void deregisterContainer() {

    }

}
