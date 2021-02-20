package de.derteufelqwe.bungeeplugin.commands;

import co.aikar.commands.BaseCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Add custom methods to BaseCommand
 */
public class DMCBaseCommand extends BaseCommand {

    /**
     * Shortcut to send a message to a sender
     * @param sender
     * @param msg
     * @param args
     */
    protected void send(CommandSender sender, String msg, Object... args) {
        sender.sendMessage(new TextComponent(String.format(msg, args)));
    }

}
