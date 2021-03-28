package de.derteufelqwe.driver.workers;

import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.LocalSessionRunnable;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Log;
import de.derteufelqwe.driver.DMCLogDriver;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.units.qual.A;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryDelegatingImpl;
import org.hibernate.internal.SessionFactoryImpl;
import org.slf4j.event.Level;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
@Log4j2
public class DatabaseWriter extends Thread {

    private SessionBuilder sessionBuilder = DMCLogDriver.getSessionBuilder();

    private final AtomicBoolean useQueue1 = new AtomicBoolean(true);
    private AtomicBoolean doRun = new AtomicBoolean(true);

    private Queue<Log> logQueue1 = new ConcurrentLinkedQueue<>();
    private Queue<Log> logQueue2 = new ConcurrentLinkedQueue<>();


    public DatabaseWriter() {

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
        session.persist(log);

        for (Log stacktrace : log.getStacktrace()) {
            session.persist(stacktrace);
        }

        if (log.getCausedBy() != null) {
            this.persistException(session, log.getCausedBy());
        }
    }


    private void flush(Queue<Log> queue) {
        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                Log log = queue.poll();
                while (log != null) {
                    session.persist(log);
                    log = queue.poll();
                }
            }
        }.run();
    }

    @SneakyThrows
    @Override
    public void run() {
        while (doRun.get() && !this.isInterrupted()) {
            this.flushCurrent();

            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                log.error("Database writer sleep interrupted. Exiting.");
            }
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

    @Override
    public synchronized void start() {
        super.start();
        log.info("Started DatabaseWriter thread.");
    }


    @Override
    public void interrupt() {
        this.doRun.set(false);
        super.interrupt();
        sessionBuilder.close();
    }
}
