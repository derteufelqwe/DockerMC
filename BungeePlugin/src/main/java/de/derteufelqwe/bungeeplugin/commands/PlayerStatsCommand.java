package de.derteufelqwe.bungeeplugin.commands;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.PlayerData;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.KeybindComponent;
import net.md_5.bungee.api.chat.SelectorComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import org.hibernate.Session;

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

        if (args.size() == 0) {
            this.printHelp(sender);

        } else if (args.size() == 1) {
            String name = args.get(0);

            if (!name.equals(sender.getName()) && !sender.hasPermission("dockermc.bungee.playerstats.others")) {
                sender.sendMessage(format(ChatColor.RED + "You aren't allowed to display other peoples stats."));

            } else {
                this.printGeneralInfos(sender, name);

            }


        } else {

        }

    }

    private TextComponent format(String msg, Object... args) {
        return new TextComponent(String.format(msg, args));
    }

    private void printHelp(CommandSender sender) {
        sender.sendMessage(this.format(ChatColor.RED + "/playerstats <username>"));
    }

    private void printGeneralInfos(CommandSender sender, String playerName) {
        PlayerData playerData = this.redisDataManager.getPlayer(playerName);

        try (Session session = this.sessionBuilder.openSession()) {

            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<DBPlayer> cq = cb.createQuery(DBPlayer.class);
            Root<DBPlayer> root = cq.from(DBPlayer.class);

            cq.select(root).where(cb.equal(root.get("name"), playerName));

            // --- Execute the query ---

            DBPlayer dbPlayer = null;

            Query queryRes = session.createQuery(cq);
            try {
                dbPlayer = (DBPlayer) queryRes.getSingleResult();

            } catch (NoResultException e) {
                sender.sendMessage(this.format(PREFIX + ChatColor.RED + "Player %s not found.", playerName));

                return;
            }

            String banned = dbPlayer.getActiveBan() == null ? ChatColor.GREEN + "No" : ChatColor.RED + "Yes";
            String online = playerData == null ? ChatColor.WHITE + "No" : ChatColor.GREEN + "Yes";

            sender.sendMessage(this.format("%s-----------  Player information  -----------", ChatColor.GOLD));
            sender.sendMessage(format(ChatColor.YELLOW + "Name:%s  %s", ChatColor.RESET, dbPlayer.getName()));
            sender.sendMessage(format(ChatColor.YELLOW + "UUID:%s  %s", ChatColor.RESET, dbPlayer.getUuid()));
            sender.sendMessage(format(ChatColor.YELLOW + "Online:%s  %s", ChatColor.RESET, online));
            if (playerData != null) {
                sender.sendMessage(format(ChatColor.YELLOW + "Proxy:%s  %s", ChatColor.RESET, playerData.getBungeeCordId()));
                sender.sendMessage(format(ChatColor.YELLOW + "Server:%s  %s", ChatColor.RESET, playerData.getServer()));
                sender.sendMessage(format(ChatColor.YELLOW + "IP:%s  %s", ChatColor.RESET, playerData.getAddress()));
            }
            sender.sendMessage(format(ChatColor.YELLOW + "First joined:%s   %s", ChatColor.RESET, Utils.formatTimestamp(dbPlayer.getFirstJoinDate())));
            sender.sendMessage(format(ChatColor.YELLOW + "Last seen:%s  %s", ChatColor.RESET, Utils.formatTimestamp(dbPlayer.getLastOnline())));
            sender.sendMessage(format(ChatColor.YELLOW + "Banned:%s  %s", ChatColor.RESET, banned));
            sender.sendMessage(format(ChatColor.YELLOW + "Bans:%s  %s", ChatColor.RESET, dbPlayer.getGottenBans().size()));
            sender.sendMessage(format(ChatColor.YELLOW + "Logins:%s  %s", ChatColor.RESET, dbPlayer.getLogins().size()));
            sender.sendMessage(format(ChatColor.YELLOW + "Playtime:%s  %s", ChatColor.RESET, Utils.formatDuration(dbPlayer.getPlaytime())));

        }

    }


}
