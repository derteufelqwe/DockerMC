package de.derteufelqwe.bungeeplugin;

import com.orbitz.consul.*;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.google.common.net.HostAndPort;
import de.derteufelqwe.bungeeplugin.consul.MinecraftKeyListener;
import de.derteufelqwe.bungeeplugin.docker.DockerSignalHandler;
import de.derteufelqwe.bungeeplugin.health.HealthCheck;
import de.derteufelqwe.commons.Constants;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Collections;

public final class BungeePlugin extends Plugin {

    private Consul consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(Constants.CONSUL_HOST, Constants.CONSUL_PORT)).build();
    private AgentClient agentClient = consul.agentClient();
    private KeyValueClient keyValueClient = consul.keyValueClient();
    private CatalogClient catalogClient = consul.catalogClient();

    private HealthCheck healthCheck = new HealthCheck();
    private MetaData metaData = new MetaData();
    private MinecraftKeyListener minecraftKeyListener;
    private Events events = new Events(keyValueClient);

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
                .tags(Collections.singleton("defaultproxy"))
                .address(containerIP)
                .port(25577)
                .check(ImmutableRegCheck.builder()
                        .http("http://" + containerIP + ":8001/health")
                        .interval("10s")
                        .timeout("5s")
                        .build())
                .putMeta("ip", containerIP)
                .build();
        System.out.println("Adding Proxy " + taskName);
        agentClient.register(newService);
    }


    @Override
    public void onEnable() {
        DockerSignalHandler.listenTo("TERM");

        // -----  Events  -----
        getProxy().getPluginManager().registerListener(this, events);

        // -----  Commands  -----
        getProxy().getPluginManager().registerCommand(this, new DockerMCCommands());

        // -----  Registrations  -----
        System.out.println("Starting Minecraft listener...");
        this.minecraftKeyListener = new MinecraftKeyListener(catalogClient);

        // -----  Consul  -----
        this.healthCheck.start();
        this.registerContainer();
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

    @Override
    public void onDisable() {
        System.out.println("Removing Server " + this.metaData.getTaskName());

        this.deregisterContainer();
        this.minecraftKeyListener.stop();
        this.events.stop();

        this.healthCheck.stop();
        consul.destroy();
    }


}
