package de.derteufelqwe.bungeeplugin.commands;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.logger.DMCLogger;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheManager;

public class TestCmd extends Command {

    private DMCLogger logger = BungeePlugin.getDmcLogger();

    public TestCmd() {
        super("test");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Cache<String, String> cache = new Cache2kBuilder<String, String>() {}
                .name("testCache")
                .build();

        System.out.println("Done");
    }
}
