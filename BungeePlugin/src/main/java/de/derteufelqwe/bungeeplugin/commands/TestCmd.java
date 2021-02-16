package de.derteufelqwe.bungeeplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.eventhandlers.PermissionEvent;
import de.derteufelqwe.commons.logger.DatabaseAppender;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.db.jdbc.ColumnConfig;
import org.apache.logging.log4j.core.appender.db.jdbc.ConnectionSource;
import org.apache.logging.log4j.core.appender.db.jdbc.DataSourceConnectionSource;
import org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender;
import org.apache.logging.log4j.core.async.AsyncLogger;

import java.util.Timer;
import java.util.TimerTask;

@CommandAlias("test")
public class TestCmd extends BaseCommand {

    private AsyncLogger logger = (AsyncLogger) LogManager.getLogger("BungeeCord");

    @Default
    public void test(CommandSender sender) {

        try {
            logger.removeAppender(logger.getAppenders().get("DatabaseLogs"));

        } catch (NullPointerException e) {}

        Appender appender = new DatabaseAppender(BungeePlugin.getSessionBuilder(), BungeePlugin.META_DATA.getContainerId(), "DatabaseLogs");
        appender.start();

        logger.addAppender(appender);
    }

    @Subcommand("a")
    public void testA(CommandSender sender) {
        logger.error("Hallo Welt");
        logger.info("Ich bin eine Info");
    }

}
