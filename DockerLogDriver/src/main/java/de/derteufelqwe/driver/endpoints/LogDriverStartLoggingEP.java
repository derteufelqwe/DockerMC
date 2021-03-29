package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.commons.hibernate.LocalSessionRunnable;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.driver.DMCLogDriver;
import de.derteufelqwe.driver.messages.LogDriver;
import de.derteufelqwe.driver.workers.LogConsumer;
import de.derteufelqwe.driver.workers.LogDownloadEntry;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.NoResultException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Log4j2
public class LogDriverStartLoggingEP extends Endpoint<LogDriver.RStartLogging, LogDriver.StartLogging> {

    /**
     * Time in ms to wait for the container to appear in the DB
     */
    private final int CONTAINER_DB_AWAIT_TIMEOUT = 20000;

    private final SessionBuilder sessionBuilder = DMCLogDriver.getSessionBuilder();
    private final ExecutorService threadPool = DMCLogDriver.getThreadPool();
    private final Map<String, LogDownloadEntry> logfileConsumers = DMCLogDriver.getLogfileConsumers();


    public LogDriverStartLoggingEP(String data) {
        super(data);
    }

    @Override
    protected LogDriver.StartLogging process(LogDriver.RStartLogging request) {
        String file = request.getFile();
        String containerID = request.getInfo().getContainerID();

        if (!this.awaitContainerInDB(containerID)) {
            return new LogDriver.StartLogging(String.format("Failed to find container %s in the DB after %s ms.", containerID, CONTAINER_DB_AWAIT_TIMEOUT));
        }

        LogConsumer logConsumer = new LogConsumer(file, containerID);
        Future<?> future = threadPool.submit(logConsumer);
        logfileConsumers.put(file, new LogDownloadEntry(logConsumer, future));

        try {
            TimeUnit.MILLISECONDS.sleep(250);

        } catch (InterruptedException e) {
            log.error("StartLogging sleep interrupted.");
        }

        return new LogDriver.StartLogging();
    }

    @Override
    protected Class<? extends Serializable> getRequestType() {
        return LogDriver.RStartLogging.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return LogDriver.StartLogging.class;
    }

    /**
     * Polls the DB for the container to have an entry in it
     *
     * @param containerID
     */
    private boolean awaitContainerInDB(String containerID) {
        long tStart = System.currentTimeMillis();

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                while ((System.currentTimeMillis() - tStart) < CONTAINER_DB_AWAIT_TIMEOUT) {
                    try {
                        String res = (String) session.createNativeQuery(
                                "SELECT id FROM containers AS c WHERE c.id = :id"
                        ).setParameter("id", containerID)
                                .getSingleResult();
                        return true;

                    } catch (NoResultException ignored) {
                    }

                    try {
                        TimeUnit.MILLISECONDS.sleep(250);

                    } catch (InterruptedException e) {
                        return false;
                    }
                }

            } finally {
                tx.commit();
            }

        }

        return false;
    }

    /**
     * DEBUG ONLY!
     * Adds the container to the DB. This is useful when the container doesn't get added by the NodeWatcher because it's
     * a non dockermc test container.
     *
     * @param info
     */
    private void injectContainerToDB(LogDriver.RStartLogging.Info info) {
        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                Node node = session.get(Node.class, "asdfasdf");
                DBService dbService = session.get(DBService.class, "debugserviceid");

                DBContainer dbContainer = new DBContainer(info.getContainerID());
                dbContainer.setName(info.getContainerName());
                dbContainer.setNode(node);
                dbContainer.setService(dbService);

                session.persist(dbContainer);

            }
        }.run();
    }

}
