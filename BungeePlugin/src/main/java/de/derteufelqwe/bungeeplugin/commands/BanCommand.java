package de.derteufelqwe.bungeeplugin.commands;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.exceptions.NotFoundException;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.PlayerBan;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;
import org.apache.commons.cli.*;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Bans a player from the whole network
 */
public class BanCommand extends Command {

    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();
    private SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();
    private static String PREFIX = ChatColor.YELLOW + "[DMCBan] " + ChatColor.RESET;


    public BanCommand() {
        super("ban", "dockermc.bungee.ban");
    }


    @Override
    public void execute(CommandSender sender, String[] rawArgs) {
        List<String> args = Arrays.asList(rawArgs);

        if (args.size() < 3) {
            this.printHelp(sender);

        } else {
            String player = args.get(0);
            long duration;
            String reason = String.join(" ", args.subList(2, args.size()));

            try {
                duration = this.parseDuration(args.get(1));

            } catch (RuntimeException e) {
                sender.sendMessage(new TextComponent(ChatColor.RED + args.get(1) + " is invalid. Must be 'days:hours:minutes' or 'inf'."));
                return;
            }

            this.banPlayer(sender, player, duration, reason);
        }

    }


    private void printHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(ChatColor.RED + "/ban <username> <duration> <reason>"));
    }


    /**
     * Parses the duration for the format [days:hours:minutes] and returns the time in milliseconds.
     * -1 means permanent
     * @param duration
     * @return
     * @throws RuntimeException
     */
    private long parseDuration(String duration) throws RuntimeException {
        if (duration.equals("inf")) {
            return -1;
        }

        String[] splits = duration.split(":");

        try {
            assert splits.length == 4;
            long days = Long.parseLong(splits[0]);
            long hours = Long.parseLong(splits[1]);
            long minutes = Long.parseLong(splits[2]);

            assert  days >= 0;
            assert  hours >= 0;
            assert  minutes >= 0;

            return (days * 12 * 60 * 60 + hours * 60 * 60 + minutes * 60) * 1000;

        } catch (NumberFormatException | AssertionError e) {
            throw new RuntimeException("Invalid input.");
        }

    }

    /**
     * Gets the user executing the command. Can be a player or the console user
     * @param session
     * @param sender
     * @return
     */
    private DBPlayer getExecutorUser(Session session, CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            return session.get(DBPlayer.class, Constants.CONSOLE_USER_UUID);
        }

        return session.get(DBPlayer.class, ((ProxiedPlayer) sender).getUniqueId());
    }


    private void banPlayer(CommandSender sender, String targetPlayer, long duration, String reason) {

        try (Session session = this.sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBPlayer executor = this.getExecutorUser(session, sender);
                DBPlayer target = CommonsAPI.getInstance().getPlayerFromDB(session, targetPlayer);
                if (target == null) {
                    sender.sendMessage(new TextComponent(PREFIX + ChatColor.RED + "User " + targetPlayer + " not found."));
                    return;
                }

                this.createNewBan(session, target, executor, reason, duration);

                tx.commit();

                sender.sendMessage(new TextComponent(PREFIX + ChatColor.RED + "Banned " + target.getName() + "."));

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }

            try {
                BungeePlugin.getBungeeApi().kickPlayer(targetPlayer, "You got banned!");

            } catch (NotFoundException ignored) {}
        }
    }

    private void createNewBan(Session session, DBPlayer target, DBPlayer executor, String reason, long duration) {
        PlayerBan ban;
        if (duration == -1) {
            ban = new PlayerBan(target, executor, reason);
        } else {
            ban = new PlayerBan(target, executor, reason, duration);
        }
        session.save(ban);
    }


}
