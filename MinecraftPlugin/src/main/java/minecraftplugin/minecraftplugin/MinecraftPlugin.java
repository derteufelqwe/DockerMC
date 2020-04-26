package minecraftplugin.minecraftplugin;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.google.common.net.HostAndPort;
import minecraftplugin.minecraftplugin.health.HealthCheck;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;

public final class MinecraftPlugin extends JavaPlugin {

    private final String CONSUL_SERVER_HOST = "consul_server";
    private final int CONSUL_SERVER_PORT = 8500;

    private String TASK_NAME;
    private String SERVER_NAME;
    private String CONTAINER_IP;

    private Consul consul;
    private AgentClient agentClient;
    private HealthCheck healthCheck = new HealthCheck();


    /**
     * Setup
     */
    private void setup() {
        TASK_NAME = System.getenv("TASK_NAME");
        SERVER_NAME = System.getenv("SERVER_NAME");
        CONTAINER_IP = Utils.getIpMap().get("eth0");

        if (TASK_NAME == null || TASK_NAME.equals("")) {
            System.err.println("[Fatal Error] Environment variable TASK_NAME can't be null.");
            getServer().shutdown();
        }

        if (SERVER_NAME == null || SERVER_NAME.equals("")) {
            System.err.println("[Fatal Error] Environment variable SERVER_NAME can't be null.");
            getServer().shutdown();
        }

        if (CONTAINER_IP == null || CONTAINER_IP.equals("")) {
            System.err.println("[Fatal Error] Failed to get the container IP.");
            getServer().shutdown();
        }

        consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(CONSUL_SERVER_HOST, CONSUL_SERVER_PORT)).build();
        agentClient = consul.agentClient();
    }

    /**
     * Register this container to Consul
     */
    private void registerContainer() {
        Registration newService = ImmutableRegistration.builder()
                .name("minecraft")
                .id(TASK_NAME)
                .tags(Collections.singleton("defaultmc"))
                .address(CONTAINER_IP)
                .port(25565)
                .check(ImmutableRegCheck.builder()
                        .http("http://" + CONTAINER_IP + ":8001/health")
                        .interval("10s")
                        .timeout("5s")
                        .build())
                .putMeta("ip", CONTAINER_IP)
                .putMeta("serverName", SERVER_NAME)
                .build();
        System.out.println("Adding Server " + SERVER_NAME + "-" + TASK_NAME);
        agentClient.register(newService);
    }

    @Override
    public void onEnable() {
        this.setup();

        getServer().getPluginCommand("dockermc").setExecutor(new DockerMCCommands());

        healthCheck.start();
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

        healthCheck.stop();
    }


}
