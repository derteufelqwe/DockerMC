package de.derteufelqwe.bungeeplugin;

import co.aikar.commands.BungeeCommandManager;
import com.orbitz.consul.*;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.google.common.net.HostAndPort;
import de.derteufelqwe.bungeeplugin.api.BungeeAPI;
import de.derteufelqwe.bungeeplugin.commands.DebugCmd;
import de.derteufelqwe.bungeeplugin.commands.TestCmd;
import de.derteufelqwe.bungeeplugin.commands.misc.*;
import de.derteufelqwe.bungeeplugin.commands.permission.PermissionCommand;
import de.derteufelqwe.bungeeplugin.commands.permission.PermissionGroupCommand;
import de.derteufelqwe.bungeeplugin.consul.KVCacheListener;
import de.derteufelqwe.bungeeplugin.consul.ServerRegistrator;
import de.derteufelqwe.bungeeplugin.consul.ServiceCatalogListener;
import de.derteufelqwe.bungeeplugin.eventhandlers.*;
import de.derteufelqwe.bungeeplugin.health.HealthCheck;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.bungeeplugin.redis.RedisPublishListener;
import de.derteufelqwe.bungeeplugin.utils.DBCache;
import de.derteufelqwe.bungeeplugin.utils.MetaData;
import de.derteufelqwe.bungeeplugin.utils.ServerState;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.logger.DMCLogger;
import de.derteufelqwe.commons.logger.DatabaseAppender;
import de.derteufelqwe.commons.redis.RedisPool;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.async.AsyncLogger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.logging.Level;

public final class BungeePlugin extends Plugin {

    // --- Consul ---
    private Consul consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(Constants.CONSUL_HOST, Constants.CONSUL_PORT)).build();
    private AgentClient agentClient = consul.agentClient();
    private KeyValueClient keyValueClient = consul.keyValueClient();
    private CatalogClient catalogClient = consul.catalogClient();
    private ServiceCatalogListener serviceCatalogListener = new ServiceCatalogListener(this.catalogClient, "minecraft");
    private KVCacheListener kvCacheListener = new KVCacheListener(this.keyValueClient, "");

    // --- Infrastructure ---
    private final HealthCheck healthCheck = new HealthCheck();
    @Getter
    public static RedisPool redisPool;
    @Getter
    public static SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", Constants.POSTGRESDB_CONTAINER_NAME, Constants.POSTGRESDB_PORT);

    // --- Static ---
    public static Plugin PLUGIN;
    public static ServerState STATE = ServerState.STARTING;
    @Getter
    private static Config<ConfigFile> CONFIG = new Config<>(new DefaultYamlConverter(), new DefaultGsonProvider(), "plugins/BungeePlugin/config.yml", new ConfigFile());
    @Getter
    private static RedisDataManager redisDataManager;   // Manage data from and to redis
    public static final MetaData META_DATA = new MetaData();
    public static final String BUNGEECORD_ID = META_DATA.getTaskName(); // Identifies the current node
    @Getter
    private static BungeeAPI bungeeApi;
    public final DBCache DB_CACHE = new DBCache();
    @Getter
    private static DMCLogger dmcLogger;


    private BungeeCommandManager commandManager = new BungeeCommandManager(this);
    private ConnectionEvents connectionEvents;
    private RedisPublishListener redisPublishListener;


    @Override
    public void onEnable() {
        // --- Setup --
        this.addSignalHandlers();
        dmcLogger = new DMCLogger("DMCLogger", Level.WARNING, getLogger());
        BungeePlugin.PLUGIN = this;
        CONFIG.load();
        this.parseConfigFile(CONFIG.get());

        // --- Redis stuff ---
        BungeePlugin.redisPool = new RedisPool("redis");
        BungeePlugin.redisDataManager = new RedisDataManager();
        BungeePlugin.redisDataManager.init();
        this.connectionEvents = new ConnectionEvents();
        this.redisPublishListener = new RedisPublishListener();

        System.out.println("Starting publish thread");
        ProxyServer.getInstance().getScheduler().runAsync(BungeePlugin.PLUGIN, this.redisPublishListener);

        // ---  Consul Listeners  ---
        this.serviceCatalogListener.init();
        this.kvCacheListener.init();

        this.serviceCatalogListener.addListener(new ServerRegistrator());
        this.kvCacheListener.addListener(this.connectionEvents);

        this.serviceCatalogListener.start();
        this.kvCacheListener.start();

        // ---  Events  ---
        getProxy().getPluginManager().registerListener(this, connectionEvents);
        getProxy().getPluginManager().registerListener(this, new GeneralEvents());
        getProxy().getPluginManager().registerListener(this, new EventsDispatcher());
        getProxy().getPluginManager().registerListener(this, new BungeeEventsHandler());
        getProxy().getPluginManager().registerListener(this, new PermissionEvent());

        // ---  Commands  ---
        getProxy().getPluginManager().registerCommand(this, new DockerMCCommand());
        getProxy().getPluginManager().registerCommand(this, new FindCommand());
        getProxy().getPluginManager().registerCommand(this, new GlistCommand());
        getProxy().getPluginManager().registerCommand(this, new SendCommand());
        getProxy().getPluginManager().registerCommand(this, new BlistCommand(this.catalogClient));
        getProxy().getPluginManager().registerCommand(this, new KickCommand());
        getProxy().getPluginManager().registerCommand(this, new BanCommand());
        getProxy().getPluginManager().registerCommand(this, new UnbanCommand());
        getProxy().getPluginManager().registerCommand(this, new PlayerStatsCommand());
        commandManager.registerCommand(new PermissionCommand());
        commandManager.registerCommand(new PermissionGroupCommand());
        commandManager.registerCommand(new DebugCmd());
        commandManager.registerCommand(new TestCmd());

        // ---  Consul  ---
        this.healthCheck.start();
        this.registerContainer();

        // --- Misc ---
        this.setupConsoleUserInDatabase();

        BungeePlugin.STATE = ServerState.RUNNING;
        System.out.printf("[System] Server %s started successfully.\n", META_DATA.getTaskName());
    }

    @Override
    public void onDisable() {
        BungeePlugin.STATE = ServerState.STOPPING;
        System.out.printf("[System] Stopping Server %s.\n", BUNGEECORD_ID);

        this.deregisterContainer();

        // --- Stop all instances ---
        this.healthCheck.stop();
        this.kvCacheListener.stop();
        this.serviceCatalogListener.stop();
        this.consul.destroy();
        BungeePlugin.redisPool.destroy();

        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            player.disconnect(new TextComponent(ChatColor.RED + "BungeeCord Proxy shutting down!"));
        }
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
     * Parses the values in the config file
     */
    private void parseConfigFile(ConfigFile config) {
        Level level;
        try {
            level = Level.parse(config.getLogLevel());

        } catch (IllegalArgumentException e) {
            level = Level.WARNING;
            dmcLogger.severe("Invalid logLevel " + config.getLogLevel() + ". Resetting to WARNING.");
            config.setLogLevel("WARNING");
            CONFIG.save();
        }

        dmcLogger.setLevel(level);
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
     * Modifies the logger to use a database appender
     */
    @Deprecated
    private void modifyLogger() {
        AsyncLogger logger = (AsyncLogger) LogManager.getLogger("BungeeCord");

        Appender appender = new DatabaseAppender(BungeePlugin.getSessionBuilder(), BungeePlugin.META_DATA.getContainerId(), "DatabaseAppender");
        appender.start();

        logger.addAppender(appender);

//        System.setOut(new PrintStream(new LoggingOutputStream(this.getLogger(), Level.INFO), true));
//        System.setErr(new PrintStream(new LoggingOutputStream(this.getLogger(), Level.SEVERE), true));
    }


    /**
     * Register this container to Consul
     */
    private void registerContainer() {
        String containerIP = META_DATA.getContainerIP();
        keyValueClient.putValue("bungeecords/" + BUNGEECORD_ID, containerIP);

        Registration newService = ImmutableRegistration.builder()
                .name("bungeecord")
                .id(BUNGEECORD_ID)
                .addTags("defaultproxy")
                .address(containerIP)
                .port(25577)
                .check(ImmutableRegCheck.builder()
                        .http("http://" + containerIP + ":8001/health")
                        .interval("10s")
                        .timeout("5s")
                        .build())
                .addChecks(ImmutableRegCheck.builder()
                        .tcp(containerIP + ":25577")
                        .interval("10s")
                        .timeout("5s")
                        .build())
                .putMeta("ip", containerIP)
                .putMeta("name", META_DATA.getTaskName())
                .putMeta("serviceName", META_DATA.getServiceName())
                .putMeta("slot", Integer.toString(META_DATA.getSlot()))
                .putMeta("taskId", META_DATA.getTaskId())
                .build();
        System.out.println("Adding Proxy " + BUNGEECORD_ID + " to Consul.");
        agentClient.register(newService);
    }

    /**
     * Remove this container from Consul
     */
    private void deregisterContainer() {

        try {
            keyValueClient.deleteKey("bungeecords/" + BUNGEECORD_ID);

        } catch (Exception e1) {
            System.err.println(e1.getMessage());
            System.err.println("Failed to remove bungeecord key-value.");
        }

        try {
            agentClient.deregister(BUNGEECORD_ID);

        } catch (ConsulException e) {
            System.err.println(e.getMessage());
            System.out.println("This is most likely due to no TASK_NAME being set!");
        }
    }

}
