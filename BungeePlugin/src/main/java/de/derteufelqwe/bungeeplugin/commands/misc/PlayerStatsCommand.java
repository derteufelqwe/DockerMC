package de.derteufelqwe.bungeeplugin.commands.misc;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.PlayerData;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.PlayerBan;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.plugin.Command;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;

/**
 * Displays general information about a player
 * Required permission:
 * - dockermc.bungee.playerstats
 * - dockermc.bungee.playerstats.others
 */
public class PlayerStatsCommand extends Command {

    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();
    private SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();
    private String PREFIX = ChatColor.YELLOW + "[PStats] " + ChatColor.RESET;


    public PlayerStatsCommand() {
        super("playerstats", "dockermc.bungee.playerstats");
    }

    @Override
    public void execute(CommandSender sender, String[] rawArgs) {
        List<String> args = Arrays.asList(rawArgs);

        String name = "";
        String type = "";

        if (args.size() == 0) {
            this.printHelp(sender);
            return;

        } else if (args.size() == 1) {
            name = args.get(0);
            type = "infos";

        } else if (args.size() <= 2) {
            name = args.get(0);
            type = args.get(1);
        }


        switch (type) {
            case "infos":
            case "info":
                this.printGeneralInfos(sender, name);
                break;

            case "bans":
                this.printBanInfos(sender, name);
                break;

            default:
                sender.sendMessage(new TextComponent(ChatColor.RED + "Unknown subcommand " + type + "."));
                break;
        }

    }

    /**
     * Formats a String and arguments to a TextComponent
     * @param msg
     * @param args
     * @return
     */
    private TextComponent format(String msg, Object... args) {
        return new TextComponent(String.format(msg, args));
    }

    /**
     * Shortcut to send messages to a player
     * @param sender
     * @param msg
     * @param args
     */
    private void send(CommandSender sender, String msg, Object... args) {
        sender.sendMessage(this.format(msg, args));
    }

    private void printHelp(CommandSender sender) {
        sender.sendMessage(this.format(ChatColor.RED + "/playerstats <username> [stats|bans]"));
    }

    private void printGeneralInfos(CommandSender sender, String playerName) {
        PlayerData playerData = this.redisDataManager.getPlayer(playerName);

        try (Session session = this.sessionBuilder.openSession()) {

            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null) {
                sender.sendMessage(this.format(PREFIX + ChatColor.RED + "Player %s not found.", playerName));
                return;
            }

            String banned = dbPlayer.getActiveBan() == null ? ChatColor.GREEN + "No" : ChatColor.RED + "Yes";
            String online = playerData == null ? ChatColor.RED + "No" : ChatColor.GREEN + "Yes";
            String firstJoin = dbPlayer.getFirstJoinDate() == null ? "-" : Utils.formatTimestamp(dbPlayer.getFirstJoinDate());
            String lastOnline = dbPlayer.getLastOnline() == null ? "-" : Utils.formatTimestamp(dbPlayer.getLastOnline());

            // --- Print the information ---
            sender.sendMessage(this.format("%s-----------  Player information  -----------", ChatColor.GOLD));
            sender.sendMessage(format(ChatColor.YELLOW + "Name:%s  %s", ChatColor.RESET, dbPlayer.getName()));
            sender.sendMessage(format(ChatColor.YELLOW + "UUID:%s  %s", ChatColor.RESET, dbPlayer.getUuid()));
            sender.sendMessage(format(ChatColor.YELLOW + "Online:%s  %s", ChatColor.RESET, online));
            if (playerData != null) {
                TextComponent serverName = new TextComponent(ChatColor.ITALIC + playerData.getServer());
                serverName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,  new ComponentBuilder("Join server").create()));
                serverName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + playerData.getServer()));
                TextComponent server = new TextComponent(ChatColor.YELLOW + "Server: ");
                server.addExtra(serverName);

                sender.sendMessage(format(ChatColor.YELLOW + "Proxy:%s  %s", ChatColor.RESET, playerData.getBungeeCordId()));
                sender.sendMessage(server);
                sender.sendMessage(format(ChatColor.YELLOW + "IP:%s  %s", ChatColor.RESET, playerData.getAddress()));
            }
            sender.sendMessage(format(ChatColor.YELLOW + "First joined:%s   %s", ChatColor.RESET, firstJoin));
            sender.sendMessage(format(ChatColor.YELLOW + "Last seen:%s  %s", ChatColor.RESET, lastOnline));
            sender.sendMessage(format(ChatColor.YELLOW + "Banned:%s  %s", ChatColor.RESET, banned));
            sender.sendMessage(format(ChatColor.YELLOW + "Bans:%s  %s", ChatColor.RESET, dbPlayer.getGottenBans().size()));
            sender.sendMessage(format(ChatColor.YELLOW + "Logins:%s  %s", ChatColor.RESET, dbPlayer.getLogins().size()));
            sender.sendMessage(format(ChatColor.YELLOW + "Playtime:%s  %s", ChatColor.RESET, Utils.formatDuration(dbPlayer.getPlaytime())));

        }

    }

    private void printBanInfos(CommandSender sender, String playerName) {

        try (Session session = this.sessionBuilder.openSession()) {

            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null) {
                sender.sendMessage(this.format(PREFIX + ChatColor.RED + "Player %s not found.", playerName));
                return;
            }

            this.send(sender, "%s--------  Gotten bans  --------", ChatColor.GOLD);

            this.send(sender, "Status | Ban time | duration | Message");
            for (PlayerBan ban : dbPlayer.getGottenBans()) {
                String active = ban.isActive() ? ChatColor.RED + "Yes" + ChatColor.RESET : ChatColor.GREEN + "No" + ChatColor.RESET;
                String duration = ban.isPermanent() ? "inf" : Utils.formatDuration(ban.getDuration());

                TextComponent banMsg = new TextComponent(ChatColor.ITALIC + "Msg");
                banMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ban.getBanMessage()).create()));

                TextComponent msg = this.format("%s | %s | %s | ", active, Utils.formatTimestamp(ban.getBannedAt()), duration);
                msg.addExtra(banMsg);

                sender.sendMessage(msg);
            }

        }

    }

}
