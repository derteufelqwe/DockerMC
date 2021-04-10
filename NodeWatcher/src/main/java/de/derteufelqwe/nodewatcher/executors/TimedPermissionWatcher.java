package de.derteufelqwe.nodewatcher.executors;

import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.nodewatcher.NodeWatcher;

import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class TimedPermissionWatcher extends Thread {

    private Logger logger = LogManager.getLogger(getClass().getName());
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();

    private AtomicBoolean doRun = new AtomicBoolean(true);

    private long lastCleanTimestamp = 0;


    public TimedPermissionWatcher() {

    }


    @SneakyThrows
    @Override
    public void run() {
        while (this.doRun.get()) {
            try {
                TimeUnit.SECONDS.sleep(1);
                if (System.currentTimeMillis() < this.lastCleanTimestamp + 60000) {
                    continue;
                }

                this.cleanPermissions();
                this.lastCleanTimestamp = System.currentTimeMillis();

            } catch (InterruptedException e1) {
                this.doRun.set(false);
                logger.warn("Stopping TimedPermissionWatcher.");

            } catch (Exception e2) {
                logger.error("Caught exception: {}.", e2.getMessage());
                e2.printStackTrace(System.err);
                CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e2, NodeWatcher.getMetaData());
            }
        }
    }

    public void interrupt() {
        this.doRun.set(false);
    }


    /**
     * Removes all timeout permissions, which timed out by now
     */
    private void cleanPermissions() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                int permRows = session.createQuery(
                        "DELETE FROM Permission AS p WHERE p.timeout <= current_timestamp()"
                ).executeUpdate();

                int permGroupRows = session.createQuery(
                        "DELETE FROM PlayerToPermissionGroup AS p WHERE p.timeout <= current_timestamp()"
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
