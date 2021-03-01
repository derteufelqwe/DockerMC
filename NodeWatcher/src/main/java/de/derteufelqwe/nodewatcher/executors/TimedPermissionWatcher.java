package de.derteufelqwe.nodewatcher.executors;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class TimedPermissionWatcher extends Thread {

    private Logger logger = NodeWatcher.getLogger();
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();

    private boolean doRun = true;

    private long lastCleanTimestamp = 0;


    public TimedPermissionWatcher() {

    }


    @SneakyThrows
    @Override
    public void run() {
        while (this.doRun) {
            TimeUnit.SECONDS.sleep(1);
            if (System.currentTimeMillis() < this.lastCleanTimestamp + 60000) {
                continue;
            }

            this.cleanPermissions();
            this.lastCleanTimestamp = System.currentTimeMillis();
        }
    }

    public void interrupt() {
        this.doRun = false;
    }


    /**
     * Removes all timeout permissions, which timed out by now
     */
    private void cleanPermissions() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                int permRows = session.createNativeQuery(
                        "DELETE FROM permissions AS p WHERE p.timeout <= now()"
                ).executeUpdate();

                int permGroupRows = session.createNativeQuery(
                        ""
                ).executeUpdate();

                tx.commit();

                logger.debug("Deleted {} timed out permission and {} permission group assignments.", permRows, permGroupRows);

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }

        }
    }


}
