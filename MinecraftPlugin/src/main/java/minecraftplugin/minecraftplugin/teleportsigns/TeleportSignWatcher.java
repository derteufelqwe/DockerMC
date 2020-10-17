package minecraftplugin.minecraftplugin.teleportsigns;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.cache.ServiceCatalogCache;
import com.orbitz.consul.model.catalog.CatalogService;
import de.derteufelqwe.commons.consul.CacheListener;
import de.derteufelqwe.commons.consul.ICacheChangeListener;
import minecraftplugin.minecraftplugin.MinecraftPlugin;
import minecraftplugin.minecraftplugin.config.SignConfig;
import minecraftplugin.minecraftplugin.config.TPSign;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Watches if a server get restarted or if the player count changes and updates the teleport signs accordingly.
 */
public class TeleportSignWatcher implements ICacheChangeListener<String, CatalogService>, PluginMessageListener {

    private CatalogClient catalogClient;
    private ServiceCatalogCache serviceCatalogCache;
    private CacheListener<String, CatalogService> cacheListener = new CacheListener<>();
    private SignConfig signConfig;
    private List<TPSign> restartingSigns = new ArrayList<>();
    private Map<TPSign, Integer> playerCountMap = new HashMap<>();

    public TeleportSignWatcher(CatalogClient catalogClient, SignConfig signConfig) {
        this.catalogClient = catalogClient;
        this.signConfig = signConfig;

        this.cacheListener.addListener(this);
        this.serviceCatalogCache = ServiceCatalogCache.newCache(catalogClient, "minecraft");
        this.serviceCatalogCache.addListener(this.cacheListener);
    }

    public void start() {
        this.serviceCatalogCache.start();
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(MinecraftPlugin.INSTANCE, () -> {
            Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
            if (player == null) {
                return;
            }

            for (TPSign sign : this.getActiveSigns().getSigns()) {
                ByteArrayDataOutput output = ByteStreams.newDataOutput();
                output.writeUTF("PlayerCount");
                output.writeUTF(sign.getDestination());
                player.sendPluginMessage(MinecraftPlugin.INSTANCE, "BungeeCord", output.toByteArray());
            }

        }, 100, 10);
    }

    public void stop() {
        this.serviceCatalogCache.stop();
    }


    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }

        ByteArrayDataInput input = ByteStreams.newDataInput(message);
        String subChannel = input.readUTF();
        if (!subChannel.equals("PlayerCount")) {
            return;
        }

        try {
            String servername = input.readUTF();
            int count = input.readInt();

            List<TPSign> signs = this.signConfig.getSigns().stream().filter(e -> e.getDestination().equals(servername)).collect(Collectors.toList());
            for (TPSign sign : signs) {
                this.playerCountMap.put(sign, count);
                this.setServerRunning(sign);
            }
        } catch (IllegalStateException e) {
            System.err.println("Server not found.");
        }
    }

    /**
     * Returns a copy of the SignConfig which only contains currently active signs
     */
    public SignConfig getActiveSigns() {
        return new SignConfig(this.signConfig.getSigns().stream().filter(e -> !this.restartingSigns.contains(e)).collect(Collectors.toList()));
    }

    /**
     * Returns a SignConfig object with all restarting signs
     * @return
     */
    public SignConfig getInactiveSigns() {
        return new SignConfig(new ArrayList<>(this.restartingSigns));
    }

    @Override
    public void onAddEntry(String key, CatalogService value) {
        String serverName = value.getServiceMeta().get("serverName") + "-" + value.getServiceMeta().get("instanceNumber");

        TPSign tpSign = this.signConfig.getByServer(serverName);
        if (tpSign == null) {
            return;
        }

        this.setServerRunning(tpSign);
        this.restartingSigns.remove(tpSign);
    }

    @Override
    public void onModifyEntry(String key, CatalogService value) {

    }

    @Override
    public void onRemoveEntry(String key, CatalogService value) {
        String serverName = value.getServiceMeta().get("serverName") + "-" + value.getServiceMeta().get("instanceNumber");

        TPSign tpSign = this.signConfig.getByServer(serverName);
        if (tpSign == null) {
            return;
        }

        this.setServerWaiting(tpSign);
        this.restartingSigns.add(tpSign);
    }


    private void setServerWaiting(TPSign tpSign) {
        Sign sign = tpSign.getSignBlock();
        Bukkit.getScheduler().runTask(MinecraftPlugin.INSTANCE, () -> {
            sign.setLine(3, "Restarting...");
            sign.update(true);
        });
    }

    private void setServerRunning(TPSign tpSign) {
        Sign sign = tpSign.getSignBlock();
        if (sign != null) {
            Bukkit.getScheduler().runTask(MinecraftPlugin.INSTANCE, () -> {
                sign.setLine(3, String.format("%s/? Players", this.playerCountMap.getOrDefault(tpSign, 0)));
                sign.update(true);
            });

        } else {
            System.out.println("Sign is null");
        }
    }

}
