package de.derteufelqwe.commons.logger;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.PersistenceException;
import java.util.concurrent.Future;

@Deprecated
public class DatabaseAppender extends AbstractAppender {

    private String containerId;
    private final SessionBuilder sessionBuilder;


    public DatabaseAppender(SessionBuilder sessionBuilder, String containerId, String name) {
        super(name, null, null, false, null);
        this.sessionBuilder = sessionBuilder;
        this.containerId = containerId;

        // Debug only
        try (Session session = sessionBuilder.openSession()) {
            try {
                Transaction tx = session.beginTransaction();

                DBContainer container = session.get(DBContainer.class, containerId);
                if (container == null) {
                    container = new DBContainer(containerId);
                    session.persist(container);
                }

                tx.commit();

            } catch (PersistenceException e) {

            }
        }
    }


    @Override
    public void append(LogEvent event) {
//        try (Session session = sessionBuilder.openSession()) {
//            Transaction tx = session.beginTransaction();
//
//            try {
//                ContainerLog log = new ContainerLog(containerId, new Timestamp(event.getTimeMillis()), event.getLevel().toString(),
//                        event.getMessage().getFormattedMessage());
//                session.persist(log);
//
//            } catch (Exception e) {
//                tx.rollback();
//                throw e;
//
//            } finally {
//                tx.commit();
//            }
//        }
    }


    public void flushToDatabase() {

    }


    @Override
    public void stop() {
        super.stop();
        System.out.println("stop");
    }

    @Override
    protected boolean stop(Future<?> future) {
        System.out.println("stop2");
        return super.stop(future);
    }


}
