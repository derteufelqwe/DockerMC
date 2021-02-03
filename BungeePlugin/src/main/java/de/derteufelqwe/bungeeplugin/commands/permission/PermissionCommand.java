package de.derteufelqwe.bungeeplugin.commands.permission;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.permissions.Permission;
import de.derteufelqwe.commons.hibernate.objects.permissions.PermissionGroup;
import de.derteufelqwe.commons.hibernate.objects.permissions.PlayerToPermissionGroup;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.awt.image.BufferedImage;

@CommandAlias("permissions|perms")
public class PermissionCommand extends BaseCommand {

    private static final String PREFIX = ChatColor.RED + "[bPERMS] " + ChatColor.RESET;

    private final SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();


    @Default
    @CatchUnknown
    public void unknownCommand(CommandSender sender) {
        sender.sendMessage(new TextComponent(PREFIX + "Unknown subcommand."));
    }


    @Subcommand("list")
    public class ListPerms extends BaseCommand {

        private final String HEADING = PREFIX + "----- List -----";

        private final int permsPerPage = 10;


        @CatchUnknown
        public void unknown(CommandSender sender) {
            sender.sendMessage(PREFIX + "Unknown subcommand.");
        }

        @Default
        public void overview(CommandSender sender, String playerName) {
            try (Session session = sessionBuilder.openSession()) {
                DBPlayer player = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
                if (player == null) {
                    sender.sendMessage(new TextComponent(PREFIX + "Player " + playerName + " not found."));
                    return;
                }

                sender.sendMessage(HEADING);
                // Permissions
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Permissions: " + ChatColor.RESET + player.getPermissions().size()));
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Service Permissions: " + ChatColor.RESET + player.getServicePermissions().size()));
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Timed Permissions: " + ChatColor.RESET + player.getTimedPermissions().size()));
                // Group
                String mainGroup = "";
                if (player.getMainPermGroup() != null)
                    mainGroup = player.getMainPermGroup().getName();
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Permission Group: " + ChatColor.RESET + mainGroup));
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Additional Groups: " + ChatColor.RESET));
                if (player.getAdditionPermGroups() != null) {
                    for (PlayerToPermissionGroup group : player.getAdditionPermGroups()) {
                        sender.sendMessage(new TextComponent("    " + group.getPermissionGroup().getName()));
                    }
                }

            }
        }

        @Subcommand("all")
        public void listAll(CommandSender sender, String playerName, @Default("1") Integer pageNumber) {
            try (Session session = sessionBuilder.openSession()) {
                DBPlayer player = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
                if (player == null) {
                    sender.sendMessage(new TextComponent(PREFIX + "Player " + playerName + " not found."));
                    return;
                }

                int start = 0;
                int end = permsPerPage;

                if (pageNumber > 1) {
                    start = (pageNumber - 1) * permsPerPage;
                    end = pageNumber * permsPerPage;
                }

                if (end >= player.getPermissions().size()) {
                    end = player.getPermissions().size();
                }

                sender.sendMessage(new TextComponent(PREFIX + "---- Normal Permissions ----"));
                for (Permission perm : player.getPermissions().subList(start, end)) {
                    sender.sendMessage(new TextComponent(perm.getPermissionText()));
                }

            }
        }

        @Subcommand("service")
        public void listService(CommandSender sender, @Optional Integer pageNumber) {
            sender.sendMessage("service");
        }

        @Subcommand("timed")
        public void listTimed(CommandSender sender, @Optional int pageNumber) {
            sender.sendMessage("timed");
        }


    }


    @Subcommand("add")
    public class AddPerms extends BaseCommand {

        private final String HEADING = PREFIX + "----- Add -----";

        @CatchUnknown
        public void unknown(CommandSender sender) {
            sender.sendMessage(PREFIX + "Unknown subcommand.");
        }


        @Subcommand("normal")
        public void addNormalPermission(CommandSender sender, String playerName, String permission) {
            try (Session session = sessionBuilder.openSession()) {
                Transaction tx = session.beginTransaction();

                try {
                    DBPlayer player = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
                    if (player == null) {
                        sender.sendMessage(new TextComponent(PREFIX + "Player " + playerName + " not found."));
                        return;
                    }

                    if (player.hasPermission(permission)) {
                        sender.sendMessage(new TextComponent(PREFIX + "Player already has '" + permission + "'."));
                        return;
                    }

                    Permission newPerm = new Permission(permission);
                    session.persist(newPerm);

                    player.getPermissions().add(newPerm);
                    session.update(player);
                    tx.commit();

                    sender.sendMessage(new TextComponent(PREFIX + "Added '" + permission + "' to player " + playerName + "."));

                } catch (Exception e) {
                    tx.rollback();
                    throw e;
                }

            }
        }

        @Subcommand("service")
        public void addServicePermission(CommandSender sender, String playerName, String permission, String serviceName) {
            sender.sendMessage("add servicese");
        }

        @Subcommand("timed")
        public void addTimedPermission(CommandSender sender, String playerName, String permission, String endTime) {
            sender.sendMessage("add timed");
        }

    }

}
