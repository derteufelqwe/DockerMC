package de.derteufelqwe.bungeeplugin;

import com.orbitz.consul.*;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.google.common.net.HostAndPort;
import de.derteufelqwe.bungeeplugin.commands.*;
import de.derteufelqwe.bungeeplugin.consul.KVCacheListener;
import de.derteufelqwe.bungeeplugin.consul.ServiceCatalogListener;
import de.derteufelqwe.bungeeplugin.eventhandlers.*;
import de.derteufelqwe.bungeeplugin.health.HealthCheck;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.bungeeplugin.redis.RedisHandler;
import de.derteufelqwe.bungeeplugin.redis.RedisPublishListener;
import de.derteufelqwe.bungeeplugin.utils.MetaData;
import de.derteufelqwe.bungeeplugin.utils.ServerState;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public final class BungeePlugin extends Plugin {

    // --- Consul ---
    private Consul consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(Constants.CONSUL_HOST, Constants.CONSUL_PORT)).build();
    private AgentClient agentClient = consul.agentClient();
    private KeyValueClient keyValueClient = consul.keyValueClient();
    private CatalogClient catalogClient = consul.catalogClient();
    private ServiceCatalogListener serviceCatalogListener = new ServiceCatalogListener(this.catalogClient, "minecraft");
    private KVCacheListener kvCacheListener = new KVCacheListener(this.keyValueClient, "");

    // --- Infrastructure ---
    private HealthCheck healthCheck = new HealthCheck();
    @Getter
    public static RedisHandler redisHandler;
    @Getter
    public static SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", Constants.POSTGRESDB_CONTAINER_NAME, Constants.POSTGRESDB_PORT);

    // --- Static ---
    @Getter
    public static Plugin PLUGIN;
    @Getter
    public static ServerState STATE = ServerState.STARTING;
    @Getter
    private static RedisDataManager redisDataManager;   // Manage data from and to redis
    public static final MetaData META_DATA = new MetaData();
    public static final String BUNGEECORD_ID = META_DATA.getTaskName(); // Identifies the current node

    private ConnectionEvents connectionEvents;
    private RedisPublishListener redisPublishListener;


    @Override
    public void onEnable() {
        this.addSignalHandlers();
        BungeePlugin.PLUGIN = this;
        // Redis stuff
        BungeePlugin.redisHandler = new RedisHandler("redis");
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
        getProxy().getPluginManager().registerListener(this, new RedisEvents());
        getProxy().getPluginManager().registerListener(this, new EventsDispatcher());
        getProxy().getPluginManager().registerListener(this, new BungeeEventsHandler());

        // ---  Commands  ---
        getProxy().getPluginManager().registerCommand(this, new DockerMCCommand());
        getProxy().getPluginManager().registerCommand(this, new FindCommand());
        getProxy().getPluginManager().registerCommand(this, new GlistCommand());
        getProxy().getPluginManager().registerCommand(this, new SendCommand());
        getProxy().getPluginManager().registerCommand(this, new BlistCommand(this.catalogClient));

        // ---  Consul  ---
        this.healthCheck.start();
        this.registerContainer();

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
        BungeePlugin.redisHandler.destroy();

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
