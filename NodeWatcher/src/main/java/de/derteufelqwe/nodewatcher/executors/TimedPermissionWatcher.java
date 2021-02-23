package de.derteufelqwe.nodewatcher.executors;

import de.derteufelqwe.commons.exceptions.DmcAPIException;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.permissions.TimedPermission;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Rmoves all {@link de.derteufelqwe.commons.hibernate.objects.permissions.TimedPermission}s which timed out
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
                CriteriaBuilder cb = session.getCriteriaBuilder();
                CriteriaQuery<TimedPermission> cq = cb.createQuery(TimedPermission.class);
                Root<TimedPermission> root = cq.from(TimedPermission.class);

                cq.select(root).where(cb.lessThan(root.get("timeout"), new Timestamp(System.currentTimeMillis())));

                TypedQuery<TimedPermission> queryRes = session.createQuery(cq);
                List<TimedPermission> res = queryRes.getResultList();

                for (TimedPermission perm : res) {
                    session.delete(perm);
                }

                tx.commit();

                logger.debug("Deleted " + res.size() + " timed out permissions.");

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }

        }
    }


}
