package de.derteufelqwe.bungeeplugin.runnables;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.SessionRunnableBase;
import de.derteufelqwe.commons.logger.DMCLogger;
import org.hibernate.Session;

/**
 * A runnable for BungeeCords scheduler to run database commands
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
        return BungeePlugin.getSessionBuilder();
    }

    @Override
    public DMCLogger getLogger() {
        return BungeePlugin.getDmcLogger();
    }

}
