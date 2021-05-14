package de.derteufelqwe.plugin.misc;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.Log;
import de.derteufelqwe.commons.hibernate.objects.NWContainer;
import de.derteufelqwe.commons.misc.RepeatingThread;
import de.derteufelqwe.plugin.DMCLogDriver;
import javassist.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;

import javax.persistence.EntityNotFoundException;
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
    private short failedTransactionCommitCounter = 0;


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

    @Override
    public void onException(Exception e) {
        log.error("Database Writer thread threw an exception.", e);
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
     * Repeated querying of the same containers does not cause a performance drop, since hibernate caches the queries session wide
     *
     * @param queue
     */
    private void flush(Queue<Log> queue) {
        List<Log> backup = new LinkedList<>(queue);

        try {
            sessionBuilder.execute(session -> {
                Log dbLog;

                while (true) {
                    dbLog = queue.poll();
                    if (dbLog == null)
                        break;

                    String containerID = "";
                    try {
                        // Replace the dummy container instance with an actual hibernate reference object
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

                    } catch (EntityNotFoundException e) {
                        log.error("Failed to save log. (NW)Container {} not found.", containerID);
                    }

                    session.persist(dbLog);
                }
            });

        } catch (TransactionException e1) {
            log.error("DB log flushing transaction failed.", e1);
            this.failedTransactionCommitCounter++;
            if (failedTransactionCommitCounter < 3) {
                // Re-add the entries to the queue if the transaction commit failed
                queue.addAll(backup);

            } else {
                log.error("Flushing logs failed 3 times. Discarding {} logs.", backup.size());
                this.failedTransactionCommitCounter = 0;
            }
        }

        this.failedTransactionCommitCounter = 0;
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
