package minecraftplugin.minecraftplugin.teleportsigns;

import de.derteufelqwe.commons.exceptions.InvalidServerName;
import minecraftplugin.minecraftplugin.MinecraftPlugin;
import minecraftplugin.minecraftplugin.config.SignConfig;
import minecraftplugin.minecraftplugin.config.TPSign;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class TeleportSignCommand implements CommandExecutor {

    /**
     * The sign is filled with the following data:
     *      [Teleport] (Can be anything)
     *      Servername (Can be anything but should represent the server you will connect to. Must be unique)
     *      World-name (Can be anything, but should represent the world nam)
     *      Player status (auto generated)
     */

    private final ChatColor COLOR = ChatColor.BLUE;
    private final ChatColor COLOR_CMD = ChatColor.YELLOW;
    private final String NAME = COLOR + "[DockerMC] " + ChatColor.RESET;

    private SignConfig signConfig;

    public TeleportSignCommand(SignConfig signConfig) {
        this.signConfig = signConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] argsArray) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;
        List<String> args = new ArrayList<>(Arrays.asList(argsArray));

        if (args.size() == 0) {
            this.helpCmd(player);
            return true;
        }

        switch (args.get(0)) {
            case "create":
                this.createCmd(player, args.subList(1, args.size()));
                break;
            case "info":
                this.infoCmd(player, args.subList(1, args.size()));
                break;

            default:
                this.helpCmd(player);
        }

        return true;
    }


    private void helpCmd(Player player) {
        player.sendMessage(COLOR + "----------  TPSigns help  ----------");
        player.sendMessage(COLOR_CMD + "create [displayed name] [server name] [world name]: " + ChatColor.RESET + "Creates a tp sign.");
        player.sendMessage(COLOR_CMD + "info: " + ChatColor.RESET + "Get information about the tp sign.");
        player.sendMessage(COLOR + "------------------------------------");
    }

    private void createCmd(Player player, List<String> args) {
        if (args.size() != 3) {
            player.sendMessage(NAME + ChatColor.RED + "/tpsign create <display name> <server> <world>");
            return;
        }

        String displayName = args.get(0);
        String serverName = args.get(1);
        String worldName = args.get(2);

        Block targetBlock = player.getTargetBlock(new HashSet<>(Arrays.asList(Material.AIR, Material.WATER, Material.LAVA)), 5);
        Material targetMaterial = targetBlock.getType();

        if (targetMaterial != Material.SIGN && targetMaterial != Material.WALL_SIGN && targetMaterial != Material.SIGN_POST) {
            player.sendMessage(NAME + ChatColor.RED + "Look at a sign.");
            return;
        }

        try {
            TPSign tpSign = new TPSign(displayName, targetBlock.getLocation(), serverName);
            this.signConfig.addSign(tpSign);
        } catch (InvalidServerName e) {

            player.sendMessage(NAME + ChatColor.RED + "Invalid destination name. It must contain one '-'.");
            return;
        }

        Sign sign = ((Sign) targetBlock.getState());
        sign.setLine(0, ChatColor.BLUE + "[Teleport Sign]");
        sign.setLine(1, ChatColor.translateAlternateColorCodes('$', displayName));
        sign.setLine(2, ChatColor.translateAlternateColorCodes('$', worldName));
        sign.setLine(3, "?/? Players");
        sign.update(true);

        player.sendMessage(String.format(NAME + COLOR_CMD + "Created TPSign %s to %s.", displayName, serverName));

        MinecraftPlugin.CONFIG.save(SignConfig.class);
    }

    private void infoCmd(Player player, List<String> args) {
        Block targetBlock = player.getTargetBlock(new HashSet<>(Arrays.asList(Material.AIR, Material.WATER, Material.LAVA)), 5);
        Material targetMaterial = targetBlock.getType();

        if (targetMaterial == null || (targetMaterial != Material.SIGN && targetMaterial != Material.WALL_SIGN && targetMaterial != Material.SIGN_POST)) {
            player.sendMessage(NAME + ChatColor.RED + "Target is not a sign.");
            return;
        }

        TPSign tpSign = this.signConfig.getAt(targetBlock.getLocation());
        if (tpSign == null) {
            player.sendMessage(NAME + ChatColor.RED + "The sign is not a teleport sign.");
            return;
        }

        player.sendMessage(ChatColor.BLUE + "-------- TPSign Info --------");
        player.sendMessage(ChatColor.YELLOW + "Name    : " + tpSign.getName());
        player.sendMessage(ChatColor.YELLOW + "Server : " + tpSign.getDestination().fullName());
        player.sendMessage(ChatColor.YELLOW + "World   : " + tpSign.getLocation().getWorld().getName());
        player.sendMessage(ChatColor.BLUE + "---------------------------");

    }

}
