package de.derteufelqwe.bungeeplugin.runnables;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import org.hibernate.Session;

/**
 * A runnable for BungeeCords scheduler to run database commands
 */
public abstract class SessionRunnable implements Runnable {

    private SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();


    @Override
    public void run() {
        try (Session session = sessionBuilder.openSession()) {
            this.run(session);
        }
    }

    public abstract void run(Session session);

}
