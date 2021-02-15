package de.derteufelqwe.bungeeplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import de.derteufelqwe.bungeeplugin.eventhandlers.PermissionEvent;
import net.md_5.bungee.api.CommandSender;

@CommandAlias("bdebug")
public class DebugCmd extends BaseCommand {

    @Subcommand("perms")
    public void permsCmd(CommandSender sender, String onOff) {
        if (onOff.equals("on")) {
            PermissionEvent.allowAll = false;
            sender.sendMessage("Enabled permission check.");

        } else if (onOff.equals("off")) {
            PermissionEvent.allowAll = true;
            sender.sendMessage("Disabled permission check.");

        }
    }

}
