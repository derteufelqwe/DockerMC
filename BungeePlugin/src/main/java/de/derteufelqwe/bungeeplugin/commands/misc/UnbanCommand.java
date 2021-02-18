package de.derteufelqwe.bungeeplugin.commands.misc;

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
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Unbans a player from the network.
 * Pardons the latest ban
 */
public class UnbanCommand extends Command {

    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();
    private SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();
    private final String PREFIX = ChatColor.YELLOW + "[DMCBan] " + ChatColor.RESET;


    public UnbanCommand() {
        super("unban", "dockermc.bungee.unban");
    }


    @Override
    public void execute(CommandSender sender, String[] rawArgs) {
        List<String> args = Arrays.asList(rawArgs);

        if (args.size() < 1) {
            this.printHelp(sender);
            return;
        }

        String player = args.get(0);
        this.unbanPlayer(sender, player);
    }


    private void printHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(ChatColor.RED + "/unban <username>"));
    }


    /**
     * Gets the user executing the command. Can be a player or the console user
     *
     * @param session
     * @param sender
     * @return
     */
    private DBPlayer getExecutorUser(Session session, CommandSender sender) {
        if (!(sender instanceof ProxiedPlayer)) {
            return session.get(DBPlayer.class, Constants.CONSOLE_USER_UUID);
        }

        return session.get(DBPlayer.class, ((ProxiedPlayer) sender).getUniqueId());
    }


    private void unbanPlayer(CommandSender sender, String targetPlayer) {

        try (Session session = this.sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBPlayer executor = this.getExecutorUser(session, sender);
                DBPlayer target = CommonsAPI.getInstance().getPlayerFromDB(session, targetPlayer);
                if (target == null) {
                    sender.sendMessage(new TextComponent(PREFIX + ChatColor.RED + "User " + targetPlayer + " not found."));
                    return;
                }

                PlayerBan ban = target.getActiveBan();
                if (ban == null) {
                    sender.sendMessage(new TextComponent(PREFIX + ChatColor.RED + "User has no active bans."));
                    return;
                }

                ban.setUnbannedBy(executor);
                ban.setUnbanTime(new Timestamp(System.currentTimeMillis()));
                session.update(ban);

                tx.commit();

                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "[DMCBan] " + ChatColor.RED + "Unbanned " + target.getName() + "."));

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }

            try {
                BungeePlugin.getBungeeApi().kickPlayer(targetPlayer, "You got banned!");

            } catch (NotFoundException ignored) {
            }
        }


    }


}
