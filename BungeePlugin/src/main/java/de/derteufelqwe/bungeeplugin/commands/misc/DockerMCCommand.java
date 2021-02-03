package de.derteufelqwe.bungeeplugin.commands.misc;

import de.derteufelqwe.bungeeplugin.health.HealthHandler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DockerMCCommand extends Command {

    private final ChatColor COLOR = ChatColor.BLUE;
    private final ChatColor COLOR_CMD = ChatColor.YELLOW;
    private final String NAME = COLOR + "[DockerMC" + ChatColor.GOLD + "B" + COLOR + "] " + ChatColor.RESET;


    public DockerMCCommand() {
        super("dockermcbungee", "", "dmcb");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (args.length == 0) {
            this.printHelp(sender);
            return;
        }

        String command = args[0];
        List<String> commandArgs = new ArrayList<>(Arrays.asList(args).subList(1, args.length));

        switch (command) {
            case "health":
                this.healthCmd(sender, commandArgs);
                break;

            default:
                this.printHelp(sender);
                break;
        }

    }


    private void printHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(COLOR + "-------  DockerMC" + ChatColor.GOLD + "B" + COLOR + " Essentials help  -------"));
        sender.sendMessage(new TextComponent(COLOR_CMD + "health [on, off]: " + ChatColor.RESET + "Force BungeeCord health check to fail."));
        sender.sendMessage(new TextComponent(COLOR + "--------------------------------------"));
    }


    private void healthCmd(CommandSender sender, List<String> args) {
        if (args.size() == 0) {
            sender.sendMessage(new TextComponent(NAME + ChatColor.RED + "Missing parameter [on, off]"));
            return;
        }

        if (args.get(0).equals("on")) {
            HealthHandler.healthy = true;
            sender.sendMessage(new TextComponent(NAME + "Set health check of BungeeCord to pass."));

        } else if (args.get(0).equals("off")) {
            HealthHandler.healthy = false;
            sender.sendMessage(new TextComponent(NAME + "Set health check of BungeeCord to fail."));

        }
    }

}
