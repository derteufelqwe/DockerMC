package minecraftplugin.minecraftplugin.teleportsigns;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import minecraftplugin.minecraftplugin.MinecraftPlugin;
import minecraftplugin.minecraftplugin.config.SignConfig;
import minecraftplugin.minecraftplugin.config.TPSign;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Sign;

import java.util.Arrays;
import java.util.List;

public class TeleportSignEvents implements Listener {

    private final List<Material> SIGN_MATERIALS = Arrays.asList(Material.SIGN, Material.WALL_SIGN, Material.SIGN_POST);

    private final ChatColor COLOR = ChatColor.BLUE;
    private final ChatColor COLOR_CMD = ChatColor.YELLOW;
    private final String NAME = COLOR + "[DockerMC] " + ChatColor.RESET;

    private TeleportSignWatcher watcher;

    public TeleportSignEvents(TeleportSignWatcher watcher) {
        this.watcher = watcher;
    }


    /**
     * Handle players clicking on tp signs
     * @param event
     */
    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!SIGN_MATERIALS.contains(block.getType())) {
            return;
        }

        TPSign tpSign = this.watcher.getActiveSigns().getAt(block.getLocation());
        if (tpSign == null) {
            return;
        }

        player.sendMessage(NAME + "Connecting to " + tpSign.getDestination());

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(tpSign.getDestination());
        player.sendPluginMessage(MinecraftPlugin.INSTANCE, "BungeeCord", out.toByteArray());
    }


    /**
     * Removes tpsigns when the block they are attached to breaks.
     * @param event
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Directly break sign
        if (SIGN_MATERIALS.contains(block.getType())) {
            this.removeSign(player, block);
        }

        // A sign will get destroyed when destroying the block
        Block b;
        b = block.getRelative(BlockFace.UP);
        if (b.getType().equals(Material.SIGN_POST)) {
            this.removeSign(player, b);
        }
        b = block.getRelative(BlockFace.NORTH);
        if (b.getType().equals(Material.WALL_SIGN) && ((Sign) b.getState().getData()).getFacing().equals(BlockFace.NORTH)) {
            this.removeSign(player, b);
        }
        b = block.getRelative(BlockFace.SOUTH);
        if (b.getType().equals(Material.WALL_SIGN) && ((Sign) b.getState().getData()).getFacing().equals(BlockFace.SOUTH)) {
            this.removeSign(player, b);
        }
        b = block.getRelative(BlockFace.WEST);
        if (b.getType().equals(Material.WALL_SIGN) && ((Sign) b.getState().getData()).getFacing().equals(BlockFace.WEST)) {
            this.removeSign(player, b);
        }
        b = block.getRelative(BlockFace.EAST);
        if (b.getType().equals(Material.WALL_SIGN) && ((Sign) b.getState().getData()).getFacing().equals(BlockFace.EAST)) {
            this.removeSign(player, b);
        }

    }

    /**
     * Remove a tpsign if it exists.
     * @param player Player that caused the removal
     * @param block The sign
     */
    private void removeSign(Player player, Block block) {
        SignConfig signConfig = this.watcher.getActiveSigns();

        if (signConfig.existsAt(block.getLocation())) {
            TPSign tpSign = signConfig.getAt(block.getLocation());
            player.sendMessage(String.format(NAME + ChatColor.YELLOW + "Removing tpsign %s to %s.", tpSign.getName(), tpSign.getDestination()));
            signConfig.removeSign(tpSign);
            MinecraftPlugin.CONFIG.save(SignConfig.class);
        }
    }

}
