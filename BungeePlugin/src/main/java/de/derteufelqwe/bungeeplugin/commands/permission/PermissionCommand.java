package de.derteufelqwe.bungeeplugin.commands.permission;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.permissions.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.awt.image.BufferedImage;
import java.sql.Timestamp;
import java.text.ParseException;

@CommandAlias("permissions|perms")
public class PermissionCommand extends BaseCommand {

    private static final String PREFIX = ChatColor.RED + "[bPERMS] " + ChatColor.RESET;

    private final SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();


    private void send(CommandSender sender, String msg, Object... args) {
        sender.sendMessage(new TextComponent(String.format(msg, args)));
    }


    @Default
    @CatchUnknown
    @Subcommand("help")
    public void printHelp(CommandSender sender) {
        send(sender, PREFIX + ChatColor.GOLD + "---- Perms Help ----");
        send(sender, "%s list%s -%s Lists a players permissions", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(sender, "%s add%s -%s Adds a permission to a player", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(sender, "%s remove%s -%s Removes a permission from a player", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
    }


    @Subcommand("list")
    private class ListPerms extends BaseCommand {

        private final String HEADING = PREFIX + ChatColor.GOLD + "----- List -----";

        private final int permsPerPage = 10;


        @Default
        @CatchUnknown
        @Subcommand("help")
        public void printHelp(CommandSender sender) {
            send(sender, PREFIX + ChatColor.GOLD + "---- Perms list Help ----");
            send(sender, "%s overview %s -%s Displays a permission overview", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
            send(sender, "%s normal%s -%s Lists the normal permissions", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
            send(sender, "%s service%s -%s Lists the service bound permissions", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
            send(sender, "%s timed%s -%s Lists the timed permissions", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        }

        @Subcommand("overview|o")
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
//                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Service Permissions: " + ChatColor.RESET + player.getServicePermissions().size()));
//                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Timed Permissions: " + ChatColor.RESET + player.getTimedPermissions().size()));
                // Group
//                String mainGroup = "";
//                if (player.getMainPermGroup() != null)
//                    mainGroup = player.getMainPermGroup().getName();
//                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Permission Group: " + ChatColor.RESET + mainGroup));
//                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Additional Groups: " + ChatColor.RESET));
//                if (player.getAdditionPermGroups() != null) {
//                    for (PlayerToPermissionGroup group : player.getAdditionPermGroups()) {
//                        sender.sendMessage(new TextComponent("    " + group.getPermissionGroup().getName()));
//                    }
//                }

            }
        }

        @Subcommand("normal|n")
        public void listAll(CommandSender sender, String playerName, @Default("1") int pageNumber) {
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
                    start = end / permsPerPage;
                }

                sender.sendMessage(new TextComponent(PREFIX + "---- Normal Permissions ----"));
                for (Permission perm : player.getPermissions().subList(start, end)) {
                    sender.sendMessage(new TextComponent(perm.getPermissionText()));
                }

            }
        }

        @Subcommand("service|s")
        public void listService(CommandSender sender, String playerName, @Default("1") int pageNumber) {
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

//                if (end >= player.getServicePermissions().size()) {
//                    end = player.getServicePermissions().size();
//                    start = end / permsPerPage;
//                }

                sender.sendMessage(new TextComponent(PREFIX + "---- Service Permissions ----"));
//                for (ServicePermission perm : player.getServicePermissions().subList(start, end)) {
//                    sender.sendMessage(new TextComponent(
//                            ChatColor.YELLOW + "(" + perm.getService().getName() + ") " + ChatColor.RESET + perm.getPermissionText()
//                    ));
//                }

            }
        }

        @Subcommand("timed|t")
        public void listTimed(CommandSender sender, String playerName, @Default("1") int pageNumber) {
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

//                if (end >= player.getTimedPermissions().size()) {
//                    end = player.getTimedPermissions().size();
//                    start = end / permsPerPage;
//                }
                sender.sendMessage(new TextComponent(PREFIX + "---- Timed Permissions ----"));
//                for (TimedPermission perm : player.getTimedPermissions().subList(start, end)) {
//                    sender.sendMessage(new TextComponent(
//                            ChatColor.YELLOW + "[" + Utils.formatTimestamp(perm.getTimeout()) + "] " + ChatColor.RESET + perm.getPermissionText()
//                    ));
//                }

            }
        }


    }


    @Subcommand("add")
    private class AddPerms extends BaseCommand {

        private final String HEADING = PREFIX + "----- Add -----";

        @Default
        @CatchUnknown
        @Subcommand("help")
        public void printHelp(CommandSender sender) {
            send(sender, PREFIX + ChatColor.GOLD + "---- Perms add Help ----");
            send(sender, "%s normal%s -%s Adds a normal permission", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
            send(sender, "%s service%s -%s Adds a service bound permission", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
            send(sender, "%s timed%s -%s Adds a timed permission", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
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
        public void addServicePermission(CommandSender sender, String playerName, String serviceName, String permission) {
            try (Session session = sessionBuilder.openSession()) {
                Transaction tx = session.beginTransaction();

                try {
                    DBPlayer player = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
                    if (player == null) {
                        sender.sendMessage(new TextComponent(PREFIX + "Player " + playerName + " not found."));
                        return;
                    }

                    DBService service = CommonsAPI.getInstance().getActiveServiceFromDB(session, serviceName);
                    if (service == null) {
                        sender.sendMessage(new TextComponent(PREFIX + "Service " + serviceName + " not found."));
                        return;
                    }

                    if (player.hasServicePermission(permission, service)) {
                        sender.sendMessage(new TextComponent(PREFIX + "Player already has permission '" + permission + "'."));
                        return;
                    }

                    ServicePermission newPerm = new ServicePermission(permission);
                    newPerm.setService(service);
                    session.persist(newPerm);

//                    player.getServicePermissions().add(newPerm);
                    session.update(player);
                    tx.commit();

                    sender.sendMessage(new TextComponent(PREFIX + "Added '" + permission + "' on service " + serviceName + " to player " + playerName + "."));

                } catch (Exception e) {
                    tx.rollback();
                    throw e;
                }

            }
        }

        @Subcommand("timed")
        public void addTimedPermission(CommandSender sender, String playerName, String permission, String endTime) {
            try (Session session = sessionBuilder.openSession()) {
                Transaction tx = session.beginTransaction();

                try {
                    DBPlayer player = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
                    if (player == null) {
                        sender.sendMessage(new TextComponent(PREFIX + "Player " + playerName + " not found."));
                        return;
                    }

                    // Parse the timestamp
                    Timestamp endTimestamp;
                    try {
                        endTimestamp = Utils.parseTimestamp(endTime);

                    } catch (ParseException e) {
                        sender.sendMessage(new TextComponent(PREFIX + "Invalid timestamp '" + endTime + "'. Format: dd.MM.yy-HH:mm"));
                        return;
                    }

                    // Find existing permission if available
                    TimedPermission newPerm = player.findTimedPermission(permission);
                    if (newPerm == null) {
                        newPerm = new TimedPermission(permission, endTimestamp);
                        session.persist(newPerm);

//                        player.getTimedPermissions().add(newPerm);
                        session.update(player);

                        sender.sendMessage(new TextComponent(PREFIX + "Added '" + permission + "' expiring at " + endTime + " to player " + playerName + "."));

                    } else {
                        newPerm.setTimeout(endTimestamp);
                        session.persist(newPerm);

                        sender.sendMessage(new TextComponent(PREFIX + "Updated '" + permission + "' expiring at " + endTime + " for player " + playerName + "."));
                    }

                    tx.commit();

                } catch (Exception e) {
                    tx.rollback();
                    throw e;
                }

            }
        }

    }


    @Subcommand("remove|rm")
    private class RemovePerms extends BaseCommand {

        private final String HEADING = PREFIX + "----- Remove -----";

        @Default
        @CatchUnknown
        @Subcommand("help")
        public void printHelp(CommandSender sender) {
            send(sender, PREFIX + ChatColor.GOLD + "---- Perms remove Help ----");
            send(sender, "%s normal%s -%s Removes a normal permission", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
            send(sender, "%s service%s -%s Removes a service bound permission", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
            send(sender, "%s timed%s -%s Removes a timed permission", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        }


        @Subcommand("normal|n")
        public void removeNormalPerm(CommandSender sender, String playerName, String permission) {
            try (Session session = sessionBuilder.openSession()) {
                Transaction tx = session.beginTransaction();

                try {
                    DBPlayer player = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
                    if (player == null) {
                        sender.sendMessage(new TextComponent(PREFIX + "Player " + playerName + " not found."));
                        return;
                    }

                    if (!player.hasPermission(permission)) {
                        sender.sendMessage(new TextComponent(PREFIX + "Player doesn't have permission '" + permission + "'."));
                        return;
                    }

                    Permission perm = player.findPermission(permission);
                    session.delete(perm);

                    sender.sendMessage(new TextComponent(PREFIX + "Removed '" + permission + "' from player " + playerName + "."));

                    tx.commit();

                } catch (Exception e) {
                    tx.rollback();
                    throw e;
                }
            }
        }

        @Subcommand("service|s")
        public void removeServicePerm(CommandSender sender, String playerName, String serviceName, String permission) {
            try (Session session = sessionBuilder.openSession()) {
                Transaction tx = session.beginTransaction();

                try {
                    DBPlayer player = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
                    if (player == null) {
                        sender.sendMessage(new TextComponent(PREFIX + "Player " + playerName + " not found."));
                        return;
                    }

                    DBService service = CommonsAPI.getInstance().getActiveServiceFromDB(session, serviceName);
                    if (service == null) {
                        sender.sendMessage(new TextComponent(PREFIX + "Service " + serviceName + " not found."));
                        return;
                    }

                    if (!player.hasServicePermission(permission, service)) {
                        sender.sendMessage(new TextComponent(PREFIX + "Player doesn't have permission '" + permission + "'."));
                        return;
                    }

                    ServicePermission newPerm = player.findServicePermission(service, permission);
                    session.delete(newPerm);

                    tx.commit();

                    sender.sendMessage(new TextComponent(PREFIX + "Removed '" + permission + "' on service " + serviceName + " from player " + playerName + "."));

                } catch (Exception e) {
                    tx.rollback();
                    throw e;
                }

            }
        }

        @Subcommand("timed|t")
        public void removeTimedPerm(CommandSender sender, String playerName, String permission) {
            try (Session session = sessionBuilder.openSession()) {
                Transaction tx = session.beginTransaction();

                try {
                    DBPlayer player = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
                    if (player == null) {
                        sender.sendMessage(new TextComponent(PREFIX + "Player " + playerName + " not found."));
                        return;
                    }


                    if (!player.hasTimedPermission(permission)) {
                        sender.sendMessage(new TextComponent(PREFIX + "Player doesn't have timed permission '" + permission + "'."));
                        return;
                    }

                    TimedPermission oldPerm = player.findTimedPermission(permission);
                    session.delete(oldPerm);

                    tx.commit();

                    sender.sendMessage(new TextComponent(PREFIX + "Removed timed permission '" + permission + "' from player " + playerName + "."));

                } catch (Exception e) {
                    tx.rollback();
                    throw e;
                }

            }
        }

    }

}
