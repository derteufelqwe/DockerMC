package de.derteufelqwe.minecraftplugin.teleportsigns;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.derteufelqwe.minecraftplugin.MinecraftPlugin;
import de.derteufelqwe.minecraftplugin.config.SignConfig;
import de.derteufelqwe.minecraftplugin.config.TPSign;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Watches if a server get restarted or if the player count changes and updates the teleport signs accordingly.
 */
public class TeleportSignWatcher implements PluginMessageListener {

    private SignConfig signConfig = MinecraftPlugin.getSIGN_CONFIG().get();
    private Map<String, Short> maxPlayerCountMap = new HashMap<>();


    public TeleportSignWatcher() {

    }


    public void start() {
        // Update the player count for a server
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(MinecraftPlugin.INSTANCE, () -> {
            // Only request updates, if players are online, to prevent a flood of messages, when a player joins because
            // plugin messages only work if a player is online.
            Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
            if (player == null) {
                return;
            }

            for (TPSign sign : this.signConfig.getActiveSigns()) {
                ByteArrayDataOutput output = ByteStreams.newDataOutput();
                output.writeUTF("PlayerCount");
                output.writeUTF(sign.getDestination().fullName());
                player.sendPluginMessage(MinecraftPlugin.INSTANCE, "BungeeCord", output.toByteArray());
            }

        }, 100, 10);
    }

    public void stop() {

    }

    /**
     * Receiver for the plugin messages.
     * This method updates all TPSigns, which connect to the server, where the response comes from
     */
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

            for (TPSign sign : this.signConfig.getByServer(servername)) {
                sign.setPlayerCount((short) count);
                this.setServerRunning(sign);
            }
        } catch (IllegalStateException e) {
            System.err.println("Server not found.");
        }
    }

    /**
     * Updates the TPSign with the newest player numbers
     */
    private void setServerRestarting(TPSign tpSign) {
        Sign sign = tpSign.getSignBlock();
        Bukkit.getScheduler().runTask(MinecraftPlugin.INSTANCE, () -> {
            sign.setLine(3, "Restarting...");
            sign.update(true);
        });
    }

    /**
     * Updates the TPSign to represent a restarting server
     */
    private void setServerRunning(TPSign tpSign) {
        Sign sign = tpSign.getSignBlock();
        if (sign != null) {
            Bukkit.getScheduler().runTask(MinecraftPlugin.INSTANCE, () -> {
                short maxPlayerCount = this.getMaxPlayerCountForServer(tpSign.getDestination().getServerName());
                sign.setLine(3, String.format("%s/%s Players", tpSign.getPlayerCount(), maxPlayerCount == -1 ? "?" : maxPlayerCount));
                sign.update(true);
            });

        } else {
            System.err.println("[Error] Sign is null");
        }
    }

    /**
     * Gets the max player count for a server from Consul and caches it. If no cache entry and no Consul entry was found,
     * -1 gets returned
     *
     * @return A short or -1
     */
    private short getMaxPlayerCountForServer(String serverName) {
        if (this.maxPlayerCountMap.containsKey(serverName)) {
            return this.maxPlayerCountMap.get(serverName);
        }

        return -1;
    }

}
