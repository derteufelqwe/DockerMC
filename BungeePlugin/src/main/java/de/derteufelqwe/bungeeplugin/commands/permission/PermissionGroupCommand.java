package de.derteufelqwe.bungeeplugin.commands.permission;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.permissions.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import org.hibernate.Session;

@CommandAlias("permissiongroup|permgroup|permg")
public class PermissionGroupCommand extends BaseCommand {

    private static final String PREFIX = ChatColor.RED + "[bPERMSg] " + ChatColor.RESET;

    private final SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();


    private void send(CommandSender sender, String msg, Object... args) {
        sender.sendMessage(new TextComponent(String.format(msg, args)));
    }


    @Default
    @CatchUnknown
    @Subcommand("help")
    public void printHelp(CommandSender sender) {
        send(sender, PREFIX + ChatColor.GOLD + "---- PermsG Help ----");
        send(sender, "%s list%s -%s Lists all permission groups", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(sender, "%s create%s -%s Creates a new permission group", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(sender, "%s inspect%s -%s Inspects the permissions of a group", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(sender, "%s edit%s -%s Edits a permission group", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(sender, "%s remove%s -%s Removes a permission group", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
    }


    @Subcommand("list|l")
    private class ListGroups extends BaseCommand {

        @Default
        @CatchUnknown
        public void listGroups(CommandSender sender) {
            try (Session session = sessionBuilder.openSession()) {

                send(sender, PREFIX + "%s--- Available groups ---", ChatColor.YELLOW);
                for (PermissionGroup group : CommonsAPI.getInstance().getAllPermissionGroups(session)) {
                    String parent = group.getParent() != null ? group.getParent().getName() : "None";
                    send(sender, "%s %s %s(%s%s) - %s", ChatColor.YELLOW, group.getName(), ChatColor.RESET, group.getPrefix(), ChatColor.RESET, parent);
                }

            }
        }

    }

    @Subcommand("inspect|i")
    private class InspectGroups extends BaseCommand {

        private final int permsPerPage = 10;

        @Default
        @CatchUnknown
        @Subcommand("help")
        public void printHelp(CommandSender sender) {
            send(sender, PREFIX + ChatColor.GOLD + "---- Perms list Help ----");
            send(sender, "%s overview %s -%s Displays a group overview", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
            send(sender, "%s normal%s -%s Lists the normal permissions", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
            send(sender, "%s service%s -%s Lists the service bound permissions", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
            send(sender, "%s timed%s -%s Lists the timed permissions", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        }


        @Subcommand("overview|o")
        public void inspectOverview(CommandSender sender, String groupName) {
            try (Session session = sessionBuilder.openSession()) {

                PermissionGroup group = CommonsAPI.getInstance().getPermissionGroup(session, groupName);
                if (group == null) {
                    send(sender, PREFIX + "Permission group '%s' not found.", groupName);
                    return;
                }

                send(sender, PREFIX + "%s--- Group overview ---", ChatColor.GOLD);
                // Permissions
                send(sender, "%sPrefix: %s%s", ChatColor.YELLOW, ChatColor.RESET, group.getPrefix());
                send(sender, "%sPermissions: %s%s", ChatColor.YELLOW, ChatColor.GRAY, group.getPermissions().size());
                send(sender, "%sService Permissions: %s%s", ChatColor.YELLOW, ChatColor.GRAY, group.getServicePermissions().size());
                send(sender, "%sTimed Permissions: %s%s", ChatColor.YELLOW, ChatColor.GRAY, group.getTimedPermissions().size());

                // Players
                long playersSet = CommonsAPI.getInstance().getPermissionGroupPlayerCount(session, groupName);
                send(sender, "%sUsage count: %s%s", ChatColor.YELLOW, ChatColor.GRAY, playersSet);

            }
        }


        @Subcommand("normal|n")
        public void inspectNormal(CommandSender sender, String groupName, @Default("1") int pageNumber) {
            try (Session session = sessionBuilder.openSession()) {

                PermissionGroup group = CommonsAPI.getInstance().getPermissionGroup(session, groupName);
                if (group == null) {
                    send(sender, PREFIX + "Permission group '%s' not found.", groupName);
                    return;
                }

                int start = 0;
                int end = permsPerPage;

                if (pageNumber > 1) {
                    start = (pageNumber - 1) * permsPerPage;
                    end = pageNumber * permsPerPage;
                }

                if (end >= group.getPermissions().size()) {
                    end = group.getPermissions().size();
                    start = (end / permsPerPage) * permsPerPage;
                }

                send(sender, PREFIX + "%s--- Group Permissions ---", ChatColor.GOLD);
                for (Permission perm : group.getPermissions().subList(start, end)) {
                    send(sender, perm.getPermissionText());
                }

            }
        }


        @Subcommand("service|s")
        public void inspectService(CommandSender sender, String groupName, @Default("1") int pageNumber) {
            try (Session session = sessionBuilder.openSession()) {

                PermissionGroup group = CommonsAPI.getInstance().getPermissionGroup(session, groupName);
                if (group == null) {
                    send(sender, PREFIX + "Permission group '%s' not found.", groupName);
                    return;
                }

                int start = 0;
                int end = permsPerPage;

                if (pageNumber > 1) {
                    start = (pageNumber - 1) * permsPerPage;
                    end = pageNumber * permsPerPage;
                }

                if (end >= group.getServicePermissions().size()) {
                    end = group.getServicePermissions().size();
                    start = (end / permsPerPage) * permsPerPage;
                }

                send(sender, PREFIX + "%s--- Group Service Permissions ---", ChatColor.GOLD);
                for (ServicePermission perm : group.getServicePermissions().subList(start, end)) {
                    send(sender, "%s (%s) %s%s", ChatColor.YELLOW, perm.getService().getName(), ChatColor.RESET, perm.getPermissionText());
                }

            }
        }


        @Subcommand("timed|t")
        public void inspectTimed(CommandSender sender, String groupName, @Default("1") int pageNumber) {
            try (Session session = sessionBuilder.openSession()) {

                PermissionGroup group = CommonsAPI.getInstance().getPermissionGroup(session, groupName);
                if (group == null) {
                    send(sender, PREFIX + "Permission group '%s' not found.", groupName);
                    return;
                }

                int start = 0;
                int end = permsPerPage;

                if (pageNumber > 1) {
                    start = (pageNumber - 1) * permsPerPage;
                    end = pageNumber * permsPerPage;
                }

                if (end >= group.getTimedPermissions().size()) {
                    end = group.getTimedPermissions().size();
                    start = (end / permsPerPage) * permsPerPage;
                }

                send(sender, PREFIX + "%s--- Group Timed Permissions ---", ChatColor.GOLD);
                for (TimedPermission perm : group.getTimedPermissions().subList(start, end)) {
                    send(sender, "%s [%s] %s%s", ChatColor.YELLOW, Utils.formatTimestamp(perm.getTimeout()), ChatColor.RESET, perm.getPermissionText());
                }

            }
        }

    }



}
