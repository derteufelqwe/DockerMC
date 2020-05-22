package minecraftplugin.minecraftplugin;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.config.providers.MinecraftYamlProvider;
import minecraftplugin.minecraftplugin.config.SignConfig;
import minecraftplugin.minecraftplugin.dockermc.DockerMCCommands;
import minecraftplugin.minecraftplugin.dockermc.DockerMCTabComplete;
import minecraftplugin.minecraftplugin.health.HealthCheck;
import minecraftplugin.minecraftplugin.teleportsigns.TeleportSignCommand;
import minecraftplugin.minecraftplugin.teleportsigns.TeleportSignEvents;
import minecraftplugin.minecraftplugin.teleportsigns.TeleportSignTabComplete;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;

public final class MinecraftPlugin extends JavaPlugin {

    public static MinecraftPlugin INSTANCE;
    public static Config CONFIG = new Config(new MinecraftYamlProvider());

    private Consul consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(Constants.CONSUL_HOST, Constants.CONSUL_PORT)).build();
    private AgentClient agentClient = consul.agentClient();
    private KeyValueClient kvClient = consul.keyValueClient();
    private HealthCheck healthCheck = new HealthCheck();
    private MetaData metaData = new MetaData();

    @Override
    public void onEnable() {
        INSTANCE = this;
        CONFIG.registerConfig(SignConfig.class, "plugins/MinecraftPlugin", "SignConfig.yml");
        CONFIG.loadAll();

        // -----  Plugin messaging channels  -----
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // -----  Commands  -----
        getServer().getPluginCommand("dockermc").setExecutor(new DockerMCCommands());
        getServer().getPluginCommand("dockermc").setTabCompleter(new DockerMCTabComplete());
        getServer().getPluginCommand("teleportsign").setExecutor(new TeleportSignCommand(CONFIG.get(SignConfig.class)));
        getServer().getPluginCommand("teleportsign").setTabCompleter(new TeleportSignTabComplete());

        // -----  Events  -----
        getServer().getPluginManager().registerEvents(new TeleportSignEvents(CONFIG.get(SignConfig.class)), this);

        // -----  Docker registration  -----
        healthCheck.start();
        this.registerContainer();
    }

    @Override
    public void onDisable() {
        CONFIG.saveAll();
        System.out.println("Removing Server " + this.metaData.getTaskName());

        this.deregisterContainer();

        healthCheck.stop();
    }


    /**
     * Register this container to Consul
     */
    private void registerContainer() {
        String taskName = this.metaData.getTaskName();
        String containerIp = this.metaData.getContainerIp();
        String serverName = this.metaData.getServerName();

        Registration newService = ImmutableRegistration.builder()
                .name("minecraft")
                .id(taskName)
                .tags(Collections.singleton("defaultmc"))
                .address(containerIp)
                .port(25565)
                .addChecks(ImmutableRegCheck.builder()
                        .http("http://" + containerIp + ":8001/health")
                        .interval("10s")
                        .timeout("5s")
                        .build())
                .addChecks(ImmutableRegCheck.builder()
                        .tcp(containerIp + ":25565")
                        .interval("10s")
                        .timeout("5s")
                        .build())
                .putMeta("ip", containerIp)
                .putMeta("serverName", serverName)
                .build();

        System.out.println("Adding Server " + serverName + "-" + taskName);
        agentClient.register(newService);

        kvClient.putValue("mcservers/" + serverName + "/softPlayerLimit", Integer.toString(this.metaData.getSoftPlayerLimit()));
    }

    /**
     * Remove this container from Consul
     */
    private void deregisterContainer() {
        try {
            agentClient.deregister(this.metaData.getTaskName());

        } catch (ConsulException e) {
            System.err.println(e.getMessage());
            System.out.println("This is most likely due to no TASK_NAME beeing set!");
        }

        kvClient.deleteKey("mcservers/" + this.metaData.getServerName());
    }

}
