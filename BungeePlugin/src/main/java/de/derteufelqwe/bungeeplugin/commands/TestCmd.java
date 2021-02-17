package de.derteufelqwe.bungeeplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import net.md_5.bungee.api.CommandSender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.async.AsyncLogger;

@CommandAlias("test")
public class TestCmd extends BaseCommand {

    private AsyncLogger logger = (AsyncLogger) LogManager.getLogger("BungeeCord");

    @Default
    public void test(CommandSender sender) {

    }

    @Subcommand("a")
    public void testA(CommandSender sender) {
//        logger.error("Hallo Welt");
//        logger.info("Ich bin eine Info");
//        logger.warn("Ich bin eine Warnung");
        System.out.println("Ich bin ein Printwe");
        System.err.println("Ich bin ein error Print");
    }

}
