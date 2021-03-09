package de.derteufelqwe.bungeeplugin.commands.misc;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.commons.exceptions.EntryNotFoundException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Kicks a player over the whole network no matter which server he is on.
 */
public class KickCommand extends Command {

    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();


    public KickCommand() {
        super("kick", "dockermc.bungee.kick");
    }

    @Override
    public void execute(CommandSender sender, String[] rawArgs) {
        List<String> args = Arrays.asList(rawArgs);

        if (args.size() == 0) {
            this.printHelp(sender);

        } else if (args.size() == 1) {
            this.kickPlayer(sender, args.get(0), null);

        } else {
            this.kickPlayer(sender, args.get(0), String.join(" ", args.subList(1, args.size())));
        }

    }

    private void printHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(ChatColor.RED + "/kick <username/uuid> [reason]"));
    }

    private void kickPlayer(CommandSender sender, String targetPlayer, String reason) {

        try {
            try {
                UUID uuid = UUID.fromString(targetPlayer);
                BungeePlugin.getBungeeApi().kickPlayer(uuid, reason);

            } catch (IllegalArgumentException e) {
                BungeePlugin.getBungeeApi().kickPlayer(targetPlayer, reason);
            }

            sender.sendMessage(new TextComponent(String.format(ChatColor.RED + "Kicked player %s.", targetPlayer)));

        } catch (EntryNotFoundException e) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Player " + targetPlayer + " is not online."));
        }
    }


}
