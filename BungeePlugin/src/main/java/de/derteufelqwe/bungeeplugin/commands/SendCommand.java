package de.derteufelqwe.bungeeplugin.commands;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.RedisDataCache;
import de.derteufelqwe.bungeeplugin.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;
import java.util.List;

public class SendCommand extends Command {

    private RedisDataCache redisDataCache;

    public SendCommand() {
        super("send", "bungeecord.command.send");
        this.redisDataCache = BungeePlugin.redisDataCache;
    }


    @Override
    public void execute(CommandSender sender, String[] rawArgs) {
        List<String> args = Arrays.asList(rawArgs);


        if (args.size() < 2) {
            this.printHelp(sender);
            return;
        }

        String source = args.get(0);
        String target = args.get(1);
        ServerInfo serverInfo = Utils.getServers().get(target);

        if (serverInfo == null) {
            sender.sendMessage(new TextComponent(String.format(
                    "%sTarget %s not found.", ChatColor.RED, target
            )));
            return;
        }

        if (source.equals("all")) {


        } else if (source.equals("current")) {


        } else {
            this.sendPlayer(sender, source, serverInfo);
        }

    }

    private void printHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(String.format(
                "%1$sNot enought arguments. Usage: /find <all:current:player_name> %1$s<target>" , ChatColor.RED
        )));
    }

    private void sendPlayer(CommandSender sender, String playerName, ServerInfo target) {
        RedisDataCache.PlayerData playerData = this.redisDataCache.getPlayer(playerName);
        if (playerData == null) {
            return;
        }
        

    }

}
