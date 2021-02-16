package de.derteufelqwe.commons.logger;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.ContainerLog;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.sql.Timestamp;

public class DatabaseAppender extends AbstractAppender {

    private String containerId;
    private final SessionBuilder sessionBuilder;


    public DatabaseAppender(SessionBuilder sessionBuilder, String containerId, String name) {
        super(name, null, null, false, null);
        this.sessionBuilder = sessionBuilder;
        this.containerId = containerId;
    }


    @Override
    public void append(LogEvent event) {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();


            try {
                ContainerLog log = new ContainerLog(containerId, new Timestamp(event.getTimeMillis()), event.getLevel().toString(),
                        event.getMessage().getFormattedMessage());
                session.persist(log);

            } catch (Exception e) {
                tx.rollback();
                throw e;

            } finally {
                tx.commit();
            }
        }
    }
}
