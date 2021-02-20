package de.derteufelqwe.bungeeplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.logger.DMCLogger;
import net.md_5.bungee.api.CommandSender;

@CommandAlias("test")
public class TestCmd extends BaseCommand {

    private DMCLogger logger = BungeePlugin.getDmcLogger();

    @Default
    public void test(CommandSender sender) {

    }

    @Subcommand("a")
    public void testA(CommandSender sender) {

    }

}
