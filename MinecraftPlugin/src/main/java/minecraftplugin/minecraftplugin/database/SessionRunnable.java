package minecraftplugin.minecraftplugin.database;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import minecraftplugin.minecraftplugin.MinecraftPlugin;
import org.hibernate.Session;

/**
 * A runnable for Bukkit scheduler to run database commands
 */
public abstract class SessionRunnable implements Runnable {

    private SessionBuilder sessionBuilder = MinecraftPlugin.getSessionBuilder();


    @Override
    public void run() {
        try (Session session = sessionBuilder.openSession()) {
            this.run(session);
        }
    }

    public abstract void run(Session session);

}
