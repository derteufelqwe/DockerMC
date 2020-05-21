package minecraftplugin.minecraftplugin.teleportsigns;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import minecraftplugin.minecraftplugin.MinecraftPlugin;
import minecraftplugin.minecraftplugin.config.SignConfig;
import minecraftplugin.minecraftplugin.config.TPSign;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class TeleportSignEvents implements Listener {

    private final ChatColor COLOR = ChatColor.BLUE;
    private final ChatColor COLOR_CMD = ChatColor.YELLOW;
    private final String NAME = COLOR + "[DockerMC] " + ChatColor.RESET;

    private SignConfig signConfig;

    public TeleportSignEvents(SignConfig signConfig) {
        this.signConfig = signConfig;
    }


    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!(block.getType() == Material.SIGN || block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST)) {
            return;
        }

        player.sendMessage("Clicked Sign");

        TPSign tpSign = this.signConfig.get(block.getLocation());
        if (tpSign == null) {
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(tpSign.getDestination());
        player.sendPluginMessage(MinecraftPlugin.INSTANCE, "BungeeCord", out.toByteArray());
    }

}
