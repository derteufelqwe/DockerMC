package de.derteufelqwe.bungeeplugin;

import com.orbitz.consul.*;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.google.common.net.HostAndPort;
import de.derteufelqwe.bungeeplugin.commands.DockerMCCommand;
import de.derteufelqwe.bungeeplugin.commands.FindCommand;
import de.derteufelqwe.bungeeplugin.commands.GlistCommand;
import de.derteufelqwe.bungeeplugin.commands.SendCommand;
import de.derteufelqwe.bungeeplugin.consul.*;
import de.derteufelqwe.bungeeplugin.events.ConnectionEvents;
import de.derteufelqwe.bungeeplugin.events.ServerRegistrator;
import de.derteufelqwe.bungeeplugin.health.HealthCheck;
import de.derteufelqwe.bungeeplugin.events.RedisEvents;
import de.derteufelqwe.bungeeplugin.redis.RedisPublishListener;
import de.derteufelqwe.bungeeplugin.redis.RedisDataCache;
import de.derteufelqwe.bungeeplugin.redis.RedisHandler;
import de.derteufelqwe.bungeeplugin.utils.MetaData;
import de.derteufelqwe.bungeeplugin.utils.ServerState;
import de.derteufelqwe.commons.Constants;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

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

    // --- Static ---
    @Getter public static Plugin PLUGIN;
    @Getter public static RedisHandler redisHandler;
    @Getter public static ServerState STATE = ServerState.STARTING;
    @Getter public static RedisDataCache redisDataCache;
    public static MetaData META_DATA = new MetaData();

    private ConnectionEvents connectionEvents;
    private RedisPublishListener redisPublishListener;


    @Override
    public void onEnable() {
        BungeePlugin.PLUGIN = this;
        BungeePlugin.redisHandler = new RedisHandler("redis");
        BungeePlugin.redisDataCache = new RedisDataCache(BungeePlugin.redisHandler.getJedisPool(), META_DATA.getTaskName());
        BungeePlugin.redisDataCache.init();
        this.connectionEvents = new ConnectionEvents();
        this.redisPublishListener = new RedisPublishListener(BungeePlugin.redisHandler.getJedisPool(), BungeePlugin.redisDataCache);
        this.redisPublishListener.start();

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

        // ---  Commands  ---
        getProxy().getPluginManager().registerCommand(this, new DockerMCCommand());
        getProxy().getPluginManager().registerCommand(this, new FindCommand());
        getProxy().getPluginManager().registerCommand(this, new GlistCommand());
        getProxy().getPluginManager().registerCommand(this, new SendCommand());

        // ---  Consul  ---
        this.healthCheck.start();
        this.registerContainer();

        BungeePlugin.STATE = ServerState.RUNNING;
        System.out.printf("[System] Server %s started successfully.\n", META_DATA.getTaskName());
    }

    @Override
    public void onDisable() {
        BungeePlugin.STATE = ServerState.STOPPING;
        System.out.printf("[System] Stopping Server %s.\n", this.META_DATA.getTaskName());

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
     * Register this container to Consul
     */
    private void registerContainer() {
        String taskName = this.META_DATA.getTaskName();
        String containerIP = this.META_DATA.getContainerIP();
        keyValueClient.putValue("bungeecords/" + taskName, containerIP);

        Registration newService = ImmutableRegistration.builder()
                .name("bungeecord")
                .id(taskName)
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
                .build();
        System.out.println("Adding Proxy " + taskName + " to Consul.");
        agentClient.register(newService);
    }


    /**
     * Remove this container from Consul
     */
    private void deregisterContainer() {
        String taskName = this.META_DATA.getTaskName();

        try {
            keyValueClient.deleteKey("bungeecords/" + taskName);

        } catch (Exception e1) {
            System.err.println(e1.getMessage());
            System.err.println("Failed to remove bungeecord key-value.");
        }

        try {
            agentClient.deregister(taskName);

        } catch (ConsulException e) {
            System.err.println(e.getMessage());
            System.out.println("This is most likely due to no TASK_NAME beeing set!");
        }
    }

}
