package de.derteufelqwe.minecraftplugin.database;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.SessionRunnableBase;
import de.derteufelqwe.commons.logger.DMCLogger;
import de.derteufelqwe.minecraftplugin.MinecraftPlugin;
import org.hibernate.Session;

/**
 * A runnable for Bukkit scheduler to run database commands
 */
public abstract class SessionRunnable extends SessionRunnableBase {

    public SessionRunnable() {
        super();
    }

    public SessionRunnable(int retryCounter) {
        super(retryCounter);
    }

    public SessionRunnable(int retryCounter, int retryInterval) {
        super(retryCounter, retryInterval);
    }

    @Override
    public SessionBuilder getSessionBuilder() {
        return MinecraftPlugin.getSessionBuilder();
    }

    @Override
    public DMCLogger getLogger() {
        return MinecraftPlugin.getDmcLogger();
    }
}
