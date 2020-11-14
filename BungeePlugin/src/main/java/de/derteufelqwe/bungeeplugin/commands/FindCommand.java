package de.derteufelqwe.bungeeplugin.commands;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.RedisDataCache;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;
import java.util.List;

public class FindCommand extends Command {

    private RedisDataCache redisDataCache;


    public FindCommand() {
        super("find", "bungeecord.command.find");
        this.redisDataCache = BungeePlugin.redisDataCache;
    }


    @Override
    public void execute(CommandSender sender, String[] rawArgs) {
        List<String> args = Arrays.asList(rawArgs);

        if (args.size() == 0) {
            this.printHelp(sender, args);
            return;
        }

        String username = args.get(0);
        this.findPlayer(sender, username);
    }


    private void printHelp(CommandSender sender, List<String> args) {
        sender.sendMessage(new TextComponent(ChatColor.RED + "Please supply a username."));
    }

    private void findPlayer(CommandSender sender, String username) {
        RedisDataCache.PlayerData playerData = this.redisDataCache.getPlayer(username);

        if (playerData == null) {
            sender.sendMessage(new TextComponent(String.format(
                    "Player %s%s%s is %snot%s online.", ChatColor.GREEN, username, ChatColor.RESET, ChatColor.RED, ChatColor.RESET
            )));

        } else {
            sender.sendMessage(new TextComponent(String.format(
                    "Player %1$s%2$s%3$s is online at %4$s%5$s%3$s through %4$s%6$s%3$s.", ChatColor.GREEN, username, ChatColor.RESET, ChatColor.YELLOW, playerData.getServer(), playerData.getBungeeCordId()
            )));
        }

    }

}
