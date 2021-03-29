package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Log;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class SessionTests {

    public static SessionBuilder sessionBuilder = new SessionBuilder("dockermc", "admin", "ubuntu1", 5432);

    @SneakyThrows
    public static void main(String[] args) {

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

//            Log log = new Log("hallo", new Timestamp(System.currentTimeMillis()), "asdf", Log.Source.STDOUT);
//            log.getStacktrace().add(new Log("stacktrace 1", new Timestamp(System.currentTimeMillis()), "asdf", Log.Source.STDOUT));
//            log.getStacktrace().add(new Log("stacktrace 2", new Timestamp(System.currentTimeMillis()), "asdf", Log.Source.STDOUT));
//
//            Log cause = new Log("cause", new Timestamp(System.currentTimeMillis()), "asdf", Log.Source.STDOUT);
//            cause.getStacktrace().add(new Log("cstack 1", new Timestamp(System.currentTimeMillis()), "asdf", Log.Source.STDOUT));
//            cause.getStacktrace().add(new Log("cstack 2", new Timestamp(System.currentTimeMillis()), "asdf", Log.Source.STDOUT));
//
//            log.setCausedBy(cause);
//
//            session.persist(log);
//            for (Log stack : log.getStacktrace()) {
//                session.persist(stack);
//            }
//
//            session.persist(cause);
//            for (Log stack : cause.getStacktrace()) {
//                session.persist(stack);
//            }

            Log readLog = session.get(Log.class, 9L);


            tx.commit();
        }

    }

}
