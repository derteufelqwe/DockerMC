package de.derteufelqwe.bungeeplugin;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.google.common.net.HostAndPort;
import de.derteufelqwe.bungeeplugin.consul.ConsulHandler;
import de.derteufelqwe.bungeeplugin.docker.DockerSignalHandler;
import de.derteufelqwe.bungeeplugin.health.HealthCheck;
import de.derteufelqwe.bungeeplugin.health.HealthHandler;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Collections;

public final class BungeePlugin extends Plugin {

    private final String CONSUL_SERVER_HOST = "consul_server";
    private final int CONSUL_SERVER_PORT = 8500;

    private String TASK_NAME;
    private String CONTAINER_IP;

    private Consul consul;
    private AgentClient agentClient;
    private ConsulHandler consulHandler;
    private HealthCheck healthCheck = new HealthCheck();


    /**
     * Setup
     */
    private void setup() {
        TASK_NAME = System.getenv("TASK_NAME");
        CONTAINER_IP = Utils.getIpMap().get("eth0");

        if (TASK_NAME == null || TASK_NAME.equals("")) {
            System.err.println("[Fatal Error] Environment variable TASK_NAME can't be null.");
            getProxy().stop("[Fatal Error] Environment variable TASK_NAME can't be null.");
        }

        if (CONTAINER_IP == null || CONTAINER_IP.equals("")) {
            System.err.println("[Fatal Error] Failed to get the container IP.");
            getProxy().stop("[Fatal Error] Failed to get the container IP.");
        }
    }

    /**
     * Register this container to Consul
     */
    private void registerContainer() {
        Registration newService = ImmutableRegistration.builder()
                .name("bungeecord")
                .id(TASK_NAME)
                .tags(Collections.singleton("defaultproxy"))
                .address(CONTAINER_IP)
                .port(25565)
                .check(ImmutableRegCheck.builder()
                        .http("http://" + CONTAINER_IP + ":8001/health")
                        .interval("10s")
                        .timeout("5s")
                        .build())
                .putMeta("ip", CONTAINER_IP)
                .build();
        System.out.println("Adding Proxy " + TASK_NAME);
        agentClient.register(newService);
    }


    @Override
    public void onEnable() {
        DockerSignalHandler.listenTo("TERM");
        this.setup();
        consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(CONSUL_SERVER_HOST, CONSUL_SERVER_PORT)).build();
        agentClient = consul.agentClient();

        // -----  Events  -----
        getProxy().getPluginManager().registerListener(this, new Events(consul));

        // -----  Commands  -----
        getProxy().getPluginManager().registerCommand(this, new DockerMCCommands());

        // -----  Registrations  -----
        System.out.println("Starting consul handler");
        consulHandler = new ConsulHandler(this.consul);
        consulHandler.startListener();

        this.healthCheck.start();
        this.registerContainer();
    }

    /**
     * Remove this container from Consul
     */
    private void deregisterContainer() {
        try {
            agentClient.deregister(TASK_NAME);

        } catch (ConsulException e) {
            System.err.println(e.getMessage());
            System.out.println("This is most likely due to no TASK_NAME beeing set!");
        }
    }

    @Override
    public void onDisable() {
        System.out.println("Removing Server " + TASK_NAME);

        this.deregisterContainer();

        this.healthCheck.stop();
        consulHandler.stopListener();
    }


}
