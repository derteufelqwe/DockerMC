package de.derteufelqwe.driver;

import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.LocalSessionRunnable;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Log;
import lombok.SneakyThrows;
import org.hibernate.Session;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class DatabaseWriter extends Thread {

    private SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", "ubuntu1", Constants.POSTGRESDB_PORT);

    private AtomicBoolean useQueue1 = new AtomicBoolean(true);

    private Queue<Log> logQueue1 = new ConcurrentLinkedQueue<>();
    private Queue<Log> logQueue2 = new ConcurrentLinkedQueue<>();

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

    private void flush(Queue<Log> queue) {
        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                for (Log log : queue) {
                    session.persist(log);
                }
            }
        }.run();
    }

    @SneakyThrows
    @Override
    public void run() {
        while (true) {
            // Currently filling queue 1
            if (useQueue1.get()) {
                useQueue1.set(false);
                System.out.println("Flushing queue 1");
                flush(logQueue1);

            // Currently filling queue 2
            } else {
                useQueue1.set(true);
                System.out.println("Flushing queue 2");
                flush(logQueue2);
            }

            TimeUnit.SECONDS.sleep(5);
        }
    }

    @Override
    public synchronized void start() {
        super.start();
        System.out.println("Started DatabaseWriter thread.");
    }
}
