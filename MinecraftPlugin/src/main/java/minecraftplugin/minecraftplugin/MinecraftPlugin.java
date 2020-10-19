package minecraftplugin.minecraftplugin;

import com.google.common.net.HostAndPort;
import com.google.gson.Gson;
import com.orbitz.consul.*;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import de.derteufelqwe.commons.config.providers.MinecraftGsonProvider;
import de.derteufelqwe.commons.config.providers.YamlConverter;
import minecraftplugin.minecraftplugin.config.SignConfig;
import minecraftplugin.minecraftplugin.dockermc.DockerMCCommands;
import minecraftplugin.minecraftplugin.dockermc.DockerMCTabComplete;
import minecraftplugin.minecraftplugin.health.HealthCheck;
import minecraftplugin.minecraftplugin.teleportsigns.TeleportSignCommand;
import minecraftplugin.minecraftplugin.teleportsigns.TeleportSignEvents;
import minecraftplugin.minecraftplugin.teleportsigns.TeleportSignTabComplete;
import minecraftplugin.minecraftplugin.teleportsigns.TeleportSignWatcher;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinecraftPlugin extends JavaPlugin {

    public static MinecraftPlugin INSTANCE;
    public static Config CONFIG = new Config(new DefaultYamlConverter(), new MinecraftGsonProvider());

    private Consul consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(Constants.CONSUL_HOST, Constants.CONSUL_PORT)).build();
    private AgentClient agentClient = consul.agentClient();
    private KeyValueClient kvClient = consul.keyValueClient();
    private CatalogClient catalogClient = consul.catalogClient();

    private ContainerMetaData metaData = new ContainerMetaData();
    private HealthCheck healthCheck = new HealthCheck();
    private TeleportSignWatcher teleportSignWatcher;

    
    public void debug() {
        YamlConverter converter = new DefaultYamlConverter();
        Gson gson = new MinecraftGsonProvider().getGson();
        ItemStack is = new ItemStack(Material.WOOD_SWORD, 2);
        is.addEnchantment(Enchantment.DAMAGE_ALL, 2);
        String res = converter.dumpJson(is);
        System.err.println("Result");
        System.out.println(res);
    }
    
    
    @Override
    public void onEnable() {
        this.debug();
        INSTANCE = this;
        CONFIG.registerConfig(SignConfig.class, "plugins/MinecraftPlugin", "SignConfig.yml");
        CONFIG.loadAll();
        CONFIG.get(SignConfig.class).setup();

        // -----  Listeners / Watchers  -----
        this.teleportSignWatcher = new TeleportSignWatcher(this.catalogClient, this.kvClient);
        this.teleportSignWatcher.start();

        // -----  Plugin messaging channels  -----
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this.teleportSignWatcher);

        // -----  Commands  -----
        getServer().getPluginCommand("dockermc").setExecutor(new DockerMCCommands());
        getServer().getPluginCommand("dockermc").setTabCompleter(new DockerMCTabComplete());
        getServer().getPluginCommand("teleportsign").setExecutor(new TeleportSignCommand(CONFIG.get(SignConfig.class)));
        getServer().getPluginCommand("teleportsign").setTabCompleter(new TeleportSignTabComplete());

        // -----  Events  -----
        getServer().getPluginManager().registerEvents(new TeleportSignEvents(), this);

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
     * Returns the instance counter of the server pool.
     * If the taskName is Server.2.sdiasdfla it will return 2.
     * If the taskName doesn't contain a number, null is returned
     * @param taskName Name of the server
     * @return Instance count or null
     */
    private Integer getInstanceNumber(String taskName) {
        String[] splits = taskName.split("\\.");

        if (splits.length == 3) {
            return Integer.parseInt(splits[1]);
        }

        return -1;
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
                .addTags("defaultmc")
                .addTags(serverName)
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
                .putMeta("instanceNumber", this.getInstanceNumber(taskName).toString())
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
