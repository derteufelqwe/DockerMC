package minecraftplugin.minecraftplugin;

import com.google.common.base.Utf8;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;

public final class MinecraftPlugin extends JavaPlugin {

    private Consul consul;
    private AgentClient agentClient;
    private String TASK_NAME = System.getenv("TASK_NAME");
    private HealthCheck healthCheck = new HealthCheck();
    private String ip = Utils.getIpMap().get("eth0");

    @Override
    public void onEnable() {
        if (TASK_NAME.equals("")) {
            getServer().shutdown();
        }

        consul = Consul.builder().withHostAndPort(HostAndPort.fromParts("consul_server", 8500)).build();
        agentClient = consul.agentClient();

        healthCheck.start();


        Registration newService = ImmutableRegistration.builder()
                .name("minecraft")
                .id(TASK_NAME)
                .tags(Collections.singleton("defaultmc"))
                .check(ImmutableRegCheck.builder()
                        .http("http://" + ip + ":8001/health")
                        .interval("10s")
                        .timeout("5s")
                        .build())
                .putMeta("ip", ip)
                .build();
        System.out.println("Adding Server " + TASK_NAME);
        agentClient.register(newService);
    }

    @Override
    public void onDisable() {
        System.out.println("Removing Server " + TASK_NAME);
        healthCheck.stop();

        try {
            agentClient.deregister(TASK_NAME);

        } catch (ConsulException e) {
            System.err.println(e.getMessage());
            System.out.println("This is most likely due to no TASK_NAME beeing set!");
        }
    }
}
