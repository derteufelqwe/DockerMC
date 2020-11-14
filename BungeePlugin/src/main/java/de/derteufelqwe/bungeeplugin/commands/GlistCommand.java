package de.derteufelqwe.bungeeplugin.commands;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.RedisDataCache;
import de.derteufelqwe.bungeeplugin.utils.Utils;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GlistCommand extends Command {

    private final int SERVERS_PER_PAGE = 10;
    private final String PREFIX = ChatColor.YELLOW + "[GList]" + ChatColor.RESET;
    private RedisDataCache redisDataCache;


    public GlistCommand() {
        super("glist", "bungeecord.command.list");
        System.out.println("System - glist");
        this.redisDataCache = BungeePlugin.redisDataCache;
    }

    @Override
    public void execute(CommandSender sender, String[] rawArgs) {
        List<String> args = Arrays.asList(rawArgs);

        int page = 0;
        if (args.size() > 0) {
            try {
                page = Integer.parseInt(args.get(0));

            } catch (NumberFormatException e) {
                // Pass
            }
        }
        if (page < 1)
            page = 1;

        this.glist(sender, page);
    }


    private void glist(CommandSender sender, int page) {
        Map<String, ServerInfo> servers = Utils.getServers();
        List<String> keys = new ArrayList<>(servers.keySet());
        int maxPages = (int) Math.ceil(keys.size() / (double) SERVERS_PER_PAGE);

        if (page > maxPages) {
            page = maxPages;
        }

        sender.sendMessage(new TextComponent(String.format(
                "%s --- Page %s/%s ---", this.PREFIX, page, maxPages
        )));

        // Print the server info lines
        for (int i = 0; i < SERVERS_PER_PAGE; i++) {
            int index = (SERVERS_PER_PAGE * (page - 1)) + i;
            if (index >= keys.size()) {
                break;
            }
            String serverName = keys.get(index);
            int playerCount = this.redisDataCache.getServersPlayerCount(serverName);

            sender.sendMessage(new TextComponent(String.format(
                    "%s[%s] %s(%s)", ChatColor.GREEN, serverName, ChatColor.YELLOW, playerCount
            )));
        }

        sender.sendMessage(new TextComponent(String.format(
                "%s Total players online: %s", this.PREFIX, this.redisDataCache.getOverallPlayerCount()
        )));

    }

}
