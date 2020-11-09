package de.derteufelqwe.bungeeplugin;

import com.orbitz.consul.*;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.google.common.net.HostAndPort;
import de.derteufelqwe.bungeeplugin.commands.DockerMCCommands;
import de.derteufelqwe.bungeeplugin.consul.*;
import de.derteufelqwe.bungeeplugin.events.ConnectionEvents;
import de.derteufelqwe.bungeeplugin.events.ServerRegistrator;
import de.derteufelqwe.bungeeplugin.health.HealthCheck;
import de.derteufelqwe.bungeeplugin.events.RedisEvents;
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
import redis.clients.jedis.Jedis;

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
    private MetaData metaData = new MetaData();

    // --- Static ---
    @Getter public static RedisHandler redisHandler;
    @Getter public static ServerState STATE = ServerState.STARTING;
    @Getter public static RedisDataCache redisDataCache;

    private ConnectionEvents connectionEvents;


    @Override
    public void onEnable() {
        BungeePlugin.redisHandler = new RedisHandler("redis");
        BungeePlugin.redisDataCache = new RedisDataCache(BungeePlugin.redisHandler.getJedisPool());
        BungeePlugin.redisDataCache.init();
        this.connectionEvents = new ConnectionEvents();

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
        getProxy().getPluginManager().registerCommand(this, new DockerMCCommands());

        // ---  Consul  ---
        this.healthCheck.start();
        this.registerContainer();

        BungeePlugin.STATE = ServerState.RUNNING;
        System.out.printf("[System] Server %s started successfully.\n", this.metaData.getTaskName());
    }

    @Override
    public void onDisable() {
        BungeePlugin.STATE = ServerState.STOPPING;
        System.out.printf("[System] Stopping Server %s.\n", this.metaData.getTaskName());

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
        String taskName = this.metaData.getTaskName();
        String containerIP = this.metaData.getContainerIP();
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
        String taskName = this.metaData.getTaskName();

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
