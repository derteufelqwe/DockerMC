package de.derteufelqwe.minecraftplugin.commands.dockermc;

import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import de.derteufelqwe.minecraftplugin.health.HealthHandler;
import de.derteufelqwe.minecraftplugin.commands.DMCBaseCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@CommandAlias("dockermc|dmc")
public class DockerMCCommands extends DMCBaseCommand {

    private final ChatColor COLOR = ChatColor.BLUE;
    private final ChatColor COLOR_CMD = ChatColor.YELLOW;
    private final String PREFIX = COLOR + "[DockerMC] " + ChatColor.RESET;

    public DockerMCCommands() {

    }


    @CatchUnknown
    @Subcommand("help")
    public void printHelp(CommandSender sender) {
        send(sender, PREFIX + COLOR + "-------  DockerMC Essentials help  -------");
        send(sender, "%s health [on, off] %s-%s Force health check to fail.", ChatColor.GRAY, ChatColor.RESET, ChatColor.YELLOW);
    }


    public void healthCmd(CommandSender sender, String onOff) {
        if (onOff.equals("on")) {
            HealthHandler.healthy = true;
            sender.sendMessage(PREFIX + "Set health check to pass.");

        } else if (onOff.equals("off")) {
            HealthHandler.healthy = false;
            sender.sendMessage(PREFIX + "Set health check to fail.");

        } else {
            send(sender, PREFIX + ChatColor.RED + "Invalid parameter. Must be [on, off].");
        }
    }

}
