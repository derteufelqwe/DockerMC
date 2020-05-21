package minecraftplugin.minecraftplugin.dockermc;

import minecraftplugin.minecraftplugin.health.HealthHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DockerMCCommands implements CommandExecutor {

    private final ChatColor COLOR = ChatColor.BLUE;
    private final ChatColor COLOR_CMD = ChatColor.YELLOW;
    private final String NAME = COLOR + "[DockerMC] " + ChatColor.RESET;

    public DockerMCCommands() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            this.printHelp(sender);
            return true;
        }

        String command = args[0];
        List<String> commandArgs = new ArrayList<>(Arrays.asList(args).subList(1, args.length));

        switch (command) {
            case "health":
                this.healthCmd(sender, commandArgs);
                break;

            case "version":
                sender.sendMessage("1.0");
                break;

            default:
                this.printHelp(sender);
                break;
        }

        return true;
    }


    private void printHelp(CommandSender sender) {
        sender.sendMessage(COLOR + "-------  DockerMC Essentials help  -------");
        sender.sendMessage(COLOR_CMD + "health [on, off]: " + ChatColor.RESET + "Force health check to fail.");
        sender.sendMessage(COLOR + "--------------------------------------");
    }

    private void healthCmd(CommandSender sender, List<String> args) {
        if (args.size() == 0) {
            sender.sendMessage(NAME + ChatColor.RED + "Missing parameter [on, off]");
            return;
        }

        if (args.get(0).equals("on")) {
            HealthHandler.healthy = true;
            sender.sendMessage(NAME + "Set health check to pass.");

        } else if (args.get(0).equals("off")) {
            HealthHandler.healthy = false;
            sender.sendMessage(NAME + "Set health check to fail.");

        }
    }


}
