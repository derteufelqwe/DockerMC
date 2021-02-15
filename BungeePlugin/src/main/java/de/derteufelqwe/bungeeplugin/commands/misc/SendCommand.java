package de.derteufelqwe.bungeeplugin.commands.misc;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.exceptions.RedisCacheException;
import de.derteufelqwe.bungeeplugin.redis.PlayerData;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.commons.protobuf.RedisMessages;
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

    private RedisMessages.BungeeMessageBase messageBase;

    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();


    public SendCommand() {
        super("send", "bungeecord.command.send");
        this.messageBase = RedisMessages.BungeeMessageBase.newBuilder()
                .setBungeeCordId(BungeePlugin.BUNGEECORD_ID)
                .build();
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
        PlayerData playerData = this.redisDataManager.getPlayer(playerName);
        if (playerData == null) {
            sender.sendMessage(new TextComponent(PREFIX + ChatColor.RED + "Couldn't find player " + playerName + "."));
            return;
        }

        this.sendSinglePlayer(playerData.getUuid(), playerName, target.getName());
        sender.sendMessage(new TextComponent(PREFIX + "Sending " + playerName + " to " + target.getName() + "."));
    }

    /**
     * Sends all players on the proxy
     *
     * @param sender
     * @param target
     */
    private void sendAll(CommandSender sender, ServerInfo target) {
        PlayerData caller = this.redisDataManager.getPlayer(sender.getName());
        if (caller == null)
            throw new RedisCacheException("Player %s not found in the redis cache.", sender.getName());

        List<PlayerData> targetPlayers = this.redisDataManager.getPlayersOnBungee(caller.getBungeeCordId());
        for (PlayerData player : targetPlayers) {
            this.sendSinglePlayer(player.getUuid(), player.getUsername(), target.getName());
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
        PlayerData caller = this.redisDataManager.getPlayer(sender.getName());
        if (caller == null)
            throw new RedisCacheException("Player %s not found in the redis cache.", sender.getName());

        List<PlayerData> targetPlayers = this.redisDataManager.getPlayerOnServer(caller.getServer());
        for (PlayerData player : targetPlayers) {
            this.sendSinglePlayer(player.getUuid(), player.getUsername(), target.getName());
        }

        sender.sendMessage(new TextComponent(String.format(
                "%sSent %s players from the current server to %s.", PREFIX, targetPlayers.size(), target.getName()
        )));
    }

    private void sendSinglePlayer(String uuid, String playerName, String targetServer) {
        RedisMessages.RequestPlayerSend requestPlayerSend = RedisMessages.RequestPlayerSend.newBuilder()
                .setBase(this.messageBase)
                .setUuid(RedisMessages.UUID.newBuilder()
                        .setData(uuid)
                        .build())
                .setUsername(playerName)
                .setTargetServer(targetServer)
                .build();

        redisDataManager.sendMessage(requestPlayerSend);
    }

}
