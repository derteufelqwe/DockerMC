package minecraftplugin.minecraftplugin;

import co.aikar.commands.PaperCommandManager;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.ConfigOld;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import de.derteufelqwe.commons.config.providers.MinecraftGsonProvider;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.protobuf.RedisMessages;
import de.derteufelqwe.commons.redis.RedisPool;
import lombok.Getter;
import minecraftplugin.minecraftplugin.commands.economy.*;
import minecraftplugin.minecraftplugin.config.SignConfig;
import minecraftplugin.minecraftplugin.dockermc.DockerMCCommands;
import minecraftplugin.minecraftplugin.dockermc.DockerMCTabComplete;
import minecraftplugin.minecraftplugin.economy.DMCEconomy;
import minecraftplugin.minecraftplugin.economy.DMCEconomyImpl;
import minecraftplugin.minecraftplugin.eventhandlers.MiscEventHandler;
import minecraftplugin.minecraftplugin.health.HealthCheck;
import minecraftplugin.minecraftplugin.teleportsigns.TeleportSignCommand;
import minecraftplugin.minecraftplugin.teleportsigns.TeleportSignEvents;
import minecraftplugin.minecraftplugin.teleportsigns.TeleportSignTabComplete;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.lang.reflect.Field;

public final class MinecraftPlugin extends JavaPlugin {

    @Getter public static MinecraftPlugin INSTANCE;
    public static ConfigOld CONFIG = new ConfigOld(new DefaultYamlConverter(), new MinecraftGsonProvider());
    @Getter private static SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", Constants.POSTGRESDB_CONTAINER_NAME, Constants.POSTGRESDB_PORT);
    @Getter private static RedisPool redisPool = new RedisPool("redis");

    private PaperCommandManager commandManager;

    @Getter private static ContainerMetaData metaData = new ContainerMetaData();
    private HealthCheck healthCheck = new HealthCheck();
//    private TeleportSignWatcher teleportSignWatcher;

    @Getter private static DMCEconomy economy = new DMCEconomyImpl();


    @Override
    public void onEnable() {
        this.addSignalHandlers();
        INSTANCE = this;
        CONFIG.registerConfig(SignConfig.class, "plugins/MinecraftPlugin", "SignConfig.yml");
        CONFIG.loadAll();
        CONFIG.get(SignConfig.class).setup();
        commandManager = new PaperCommandManager(this);

        // Register economy
        getServer().getServicesManager().register(Economy.class, economy, this, ServicePriority.Normal);

        // -----  Listeners / Watchers  -----
//        this.teleportSignWatcher = new TeleportSignWatcher(this.catalogClient, this.kvClient);
//        this.teleportSignWatcher.start();

        // -----  Plugin messaging channels  -----
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
//        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this.teleportSignWatcher);

        // -----  Commands  -----
        getServer().getPluginCommand("dockermc").setExecutor(new DockerMCCommands());
        getServer().getPluginCommand("dockermc").setTabCompleter(new DockerMCTabComplete());
        getServer().getPluginCommand("teleportsign").setExecutor(new TeleportSignCommand(CONFIG.get(SignConfig.class)));
        getServer().getPluginCommand("teleportsign").setTabCompleter(new TeleportSignTabComplete());
        commandManager.registerCommand(new MainBalance());
        commandManager.registerCommand(new MainPay());
        commandManager.registerCommand(new ServiceBalance());
        commandManager.registerCommand(new ServicePay());
        commandManager.registerCommand(new BankCmd());

        // -----  Events  -----
        getServer().getPluginManager().registerEvents(new MiscEventHandler(), this);
        getServer().getPluginManager().registerEvents(new TeleportSignEvents(), this);

        // -----  Docker registration  -----
        healthCheck.start();
        this.registerContainer();
    }

    @Override
    public void onDisable() {
        this.deregisterContainer();

        // This try-catch contraption enables to use Plugin messages in onDisable
        try {
            Field field = JavaPlugin.class.getDeclaredField("isEnabled");
            field.setAccessible(true);
            field.set(this, true);

            try {
                this.preDisable();

            } finally {
                field.set(this, false);
            }

        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }

        CONFIG.saveAll();
        System.out.println("Removing Server " + this.metaData.getTaskName());

        healthCheck.stop();
    }


    /**
     * Called before the actual onDisable method
     */
    public void preDisable() {
        // Connect all players to the special "toLobby" server, which redirects them to a lobby server
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.RED + "Server shutting down! Moving to lobby.");
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("toLobby");
            player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
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
                Bukkit.shutdown();
            }
        };

        Signal.handle(new Signal("TERM"), signalHandler);
        Signal.handle(new Signal("INT"), signalHandler);
        Signal.handle(new Signal("HUP"), signalHandler);
    }

    /**
     * Returns the instance counter of the server pool.
     * If the taskName is Server.2.sdiasdfla it will return 2.
     * If the taskName doesn't contain a number, null is returned
     *
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
     * Notifies the BungeeCord proxies in the network that this Minecraft server is online and ready to connect to.
     */
    private void registerContainer() {
        RedisMessages.MCServerStarted mcStarted = RedisMessages.MCServerStarted.newBuilder()
                .setContainerId(metaData.getContainerID())
                .build();

        RedisMessages.RedisMessage message = RedisMessages.RedisMessage.newBuilder()
                .setMcServerStarted(mcStarted)
                .build();

        try (Jedis jedis = redisPool.getJedisPool().getResource()) {
            jedis.publish(Constants.REDIS_MESSAGES_CHANNEL, message.toByteArray());
        }
    }


    /**
     * Notifies the BungeeCord proxies in the network that this Minecraft server is going offline and
     * won't accept any new connections.
     */
    private void deregisterContainer() {
        RedisMessages.MCServerStopped mcStopped = RedisMessages.MCServerStopped.newBuilder()
                .setContainerId(metaData.getContainerID())
                .build();

        RedisMessages.RedisMessage message = RedisMessages.RedisMessage.newBuilder()
                .setMcServerStopped(mcStopped)
                .build();

        try (Jedis jedis = redisPool.getJedisPool().getResource()) {
            jedis.publish(Constants.REDIS_MESSAGES_CHANNEL, message.toByteArray());
        }
    }

}
