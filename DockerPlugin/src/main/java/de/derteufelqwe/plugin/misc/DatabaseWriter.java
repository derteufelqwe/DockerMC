package de.derteufelqwe.plugin.misc;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.Log;
import de.derteufelqwe.commons.hibernate.objects.NWContainer;
import de.derteufelqwe.commons.misc.RepeatingThread;
import de.derteufelqwe.plugin.DMCLogDriver;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;

import javax.persistence.NoResultException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
@Log4j2
public class DatabaseWriter extends RepeatingThread {

    private final SessionBuilder sessionBuilder = DMCLogDriver.getSessionBuilder();

    private final AtomicBoolean useQueue1 = new AtomicBoolean(true);

    private final Queue<Log> logQueue1 = new ConcurrentLinkedQueue<>();
    private final Queue<Log> logQueue2 = new ConcurrentLinkedQueue<>();


    public DatabaseWriter() {
        super(5000);
    }


    @Override
    public void repeatedRun() {
        this.flushCurrent();
    }

    @Override
    public synchronized void start() {
        super.start();
        log.info("Started DatabaseWriter thread.");
    }


    /**
     * Adds a log to the queue
     *
     * @param log
     */
    public void pushLog(Log log) {
        if (useQueue1.get()) {
            this.logQueue1.add(log);

        } else {
            this.logQueue2.add(log);
        }
    }

    public void pushException(Log exceptionLog) {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                this.persistException(session, exceptionLog);

            } finally {
                tx.commit();
            }
        }
    }

    private void persistException(Session session, Log log) {
        persistLog(session, log);

        for (Log stacktrace : log.getStacktrace()) {
            persistLog(session, stacktrace);
        }

        if (log.getCausedBy() != null) {
            this.persistException(session, log.getCausedBy());
        }
    }


    /**
     * Flushes the current queue.
     * If the transaction commit fails the lost log entries get re-added to the queue.
     *
     * @param queue
     */
    private void flush(Queue<Log> queue) {
        List<Log> backup = new LinkedList<>(queue);

        try {
            sessionBuilder.execute(session -> {
                Map<String, DBContainer> dbContainerCache = new HashMap<>();
                Map<String, NWContainer> nwContainerCache = new HashMap<>();
                Log dbLog = queue.poll();

                while (dbLog != null) {
                    // Replace the dummy container instance with an actual hibernate reference object
                    if (dbLog.getContainer() != null) {
                        String containerID = dbLog.getContainer().getId();
                        DBContainer dbContainer = getFromCache(DBContainer.class, dbContainerCache, session, containerID);
                        dbLog.setContainer(dbContainer);

                    } else if (dbLog.getNwContainer() != null) {
                        String containerID = dbLog.getNwContainer().getId();
                        NWContainer nwContainer = getFromCache(NWContainer.class, nwContainerCache, session, containerID);
                        dbLog.setNwContainer(nwContainer);

                    } else {
                        log.error("Tried to save log entry, which is mapped to no container.");
                    }

                    session.persist(dbLog);
                    dbLog = queue.poll();
                }
            });

        } catch (TransactionException e1) {
            // Re-add the entries to the queue if the transaction commit failed
            queue.addAll(backup);
        }
    }

    private void flushCurrent() {
        // Currently filling queue 1
        if (useQueue1.get()) {
            useQueue1.set(false);
            log.trace("Flushing queue 1. ({} Entries)", logQueue1.size());
            flush(logQueue1);

            // Currently filling queue 2
        } else {
            useQueue1.set(true);
            log.trace("Flushing queue 2. ({} Entries)", logQueue2.size());
            flush(logQueue2);
        }
    }

    public void flushAll() {
        flushCurrent();
        flushCurrent();
    }

    // -----  Utility methods  -----

    /**
     * Tries to get a value from the provided cache or generates it if not present
     *
     * @param type
     * @param cache
     * @param session
     * @param containerID
     * @param <T>
     * @return
     */
    private <T> T getFromCache(Class<T> type, Map<String, T> cache, Session session, String containerID) {
        try {
            return cache.putIfAbsent(containerID, session.getReference(type, containerID));

        } catch (NoResultException e) {
            cache.put(containerID, null);
            return null;
        }
    }

    /**
     * Saves a log entry to the DB and replaces the dummy references to the container
     *
     * @param session
     * @param dbLog
     */
    private void persistLog(Session session, Log dbLog) {
        // Replace the dummy container instance with an actual hibernate reference object
        String containerID = null;
        try {
            if (dbLog.getContainer() != null) {
                containerID = dbLog.getContainer().getId();
                DBContainer dbContainer = session.getReference(DBContainer.class, containerID);
                dbLog.setContainer(dbContainer);

            } else if (dbLog.getNwContainer() != null) {
                containerID = dbLog.getNwContainer().getId();
                NWContainer nwContainer = session.getReference(NWContainer.class, containerID);
                dbLog.setNwContainer(nwContainer);

            } else {
                log.error("Tried to save log entry, which is mapped to no container.");
            }

            session.persist(dbLog);

        } catch (NoResultException e) {
            log.error("Failed to save log entry. Container {} not found.", containerID);
        }
    }

}
