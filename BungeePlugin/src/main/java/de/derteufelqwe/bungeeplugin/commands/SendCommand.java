package de.derteufelqwe.bungeeplugin.commands;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.RedisDataCache;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerConnectMessage;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;
import java.util.List;

/**
 * Send command, which moves player between servers.
 * Can send one player, all players on the proxy or all players on a server
 */
public class SendCommand extends Command {

    private final String PREFIX = ChatColor.YELLOW + "[Send] " + ChatColor.RESET;

    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();

    public SendCommand() {
        super("send", "bungeecord.command.send");
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
        ServerInfo serverInfo = ProxyServer.getInstance().getServersCopy().get(target);

        if (serverInfo == null) {
            sender.sendMessage(new TextComponent(String.format(
                    PREFIX + "%sTarget %s not found.", ChatColor.RED, target
            )));
            return;
        }

        if (source.equals("all")) {
            this.sendAll(sender, serverInfo);

        } else if (source.equals("current")) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ChatColor.RED + "You must be online to use this command.");
                return;
            }
            this.sendCurrent((ProxiedPlayer) sender, serverInfo);

        } else {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ChatColor.RED + "You must be online to use this command.");
                return;
            }
            this.sendPlayer((ProxiedPlayer) sender, source, serverInfo);
        }

    }

    private void printHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(String.format(
                PREFIX + "%1$sNot enough arguments. \n%1$sUsage: /send <all:current:player_name> <target>", ChatColor.RED
        )));
    }

    /**
     * Sends a single player
     *
     * @param sender
     * @param playerName
     * @param target
     */
    private void sendPlayer(ProxiedPlayer sender, String playerName, ServerInfo target) {
        RedisDataCache.PlayerData playerData = this.redisDataManager.getPlayer(playerName);
        if (playerData == null) {
            sender.sendMessage(new TextComponent(PREFIX + ChatColor.RED + "Couldn't find player " + playerName + "."));
            return;
        }

        redisDataManager.sendConnectMessage(new RedisPlayerConnectMessage(playerName, playerData.getBungeeCordId(), target.getName()));
        sender.sendMessage(new TextComponent(PREFIX + "Sending " + playerName + " to " + target.getName() + "."));
    }

    /**
     * Sends all players on the proxy
     *
     * @param sender
     * @param target
     */
    private void sendAll(CommandSender sender, ServerInfo target) {
        List<RedisDataCache.PlayerData> targetPlayers = this.redisDataManager.getPlayersOnBungee(this.redisDataManager.getPlayer(sender.getName()).getBungeeCordId());
        for (RedisDataCache.PlayerData player : targetPlayers) {
            redisDataManager.sendConnectMessage(new RedisPlayerConnectMessage(player.getUsername(), player.getBungeeCordId(), target.getName()));
        }

        sender.sendMessage(new TextComponent(String.format(
                "%sSent %s players from the current BungeeCord to %s.", PREFIX, targetPlayers.size(), target.getName()
        )));
    }

    /**
     * Sends all players on the current server
     *
     * @param sender
     * @param target
     */
    private void sendCurrent(ProxiedPlayer sender, ServerInfo target) {
        List<RedisDataCache.PlayerData> targetPlayers = this.redisDataManager.getPlayerOnServer(this.redisDataManager.getPlayer(sender.getName()).getServer());
        for (RedisDataCache.PlayerData player : targetPlayers) {
            redisDataManager.sendConnectMessage(new RedisPlayerConnectMessage(player.getUsername(), player.getBungeeCordId(), target.getName()));
        }

        sender.sendMessage(new TextComponent(String.format(
                "%sSent %s players from the current server to %s.", PREFIX, targetPlayers.size(), target.getName()
        )));
    }

}
