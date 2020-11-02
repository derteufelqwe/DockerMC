package de.derteufelqwe.bungeeplugin;

import com.orbitz.consul.*;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.google.common.net.HostAndPort;
import de.derteufelqwe.bungeeplugin.consul.MinecraftServiceListener;
import de.derteufelqwe.bungeeplugin.consul.ServerRegistrator;
import de.derteufelqwe.bungeeplugin.health.HealthCheck;
import de.derteufelqwe.commons.Constants;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public final class BungeePlugin extends Plugin {

    private Consul consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(Constants.CONSUL_HOST, Constants.CONSUL_PORT)).build();
    private AgentClient agentClient = consul.agentClient();
    private KeyValueClient keyValueClient = consul.keyValueClient();
    private CatalogClient catalogClient = consul.catalogClient();

    private HealthCheck healthCheck = new HealthCheck();
    private MetaData metaData = new MetaData();
    private MinecraftServiceListener minecraftServiceListener;
    private Events events;


    @Override
    public void onEnable() {
        this.minecraftServiceListener = new MinecraftServiceListener(catalogClient);

        // -----  Registrations  -----
        this.minecraftServiceListener.addListener(new ServerRegistrator());

        // -----  Events  -----
        this.events = new Events(keyValueClient);
        getProxy().getPluginManager().registerListener(this, events);

        // -----  Commands  -----
        getProxy().getPluginManager().registerCommand(this, new DockerMCCommands());

        this.minecraftServiceListener.start();

        // -----  Consul  -----
        this.healthCheck.start();
        this.registerContainer();
    }

    @Override
    public void onDisable() {
        System.out.println("Removing Server " + this.metaData.getTaskName());

        this.deregisterContainer();
        this.minecraftServiceListener.stop();
        this.events.stop();

        this.healthCheck.stop();
        consul.destroy();

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
