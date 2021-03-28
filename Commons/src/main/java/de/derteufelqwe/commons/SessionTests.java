package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Log;
import de.derteufelqwe.commons.hibernate.objects.permissions.Permission;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.sql.Timestamp;
import java.util.List;

public class SessionTests {

//    public static SessionBuilder sessionBuilder = new SessionBuilder("dockermc", "admin", "ubuntu1", 5432);

    @SneakyThrows
    public static void main(String[] args) {

//        try (Session session = sessionBuilder.openSession()) {
//            Transaction tx = session.beginTransaction();
//
////            Log log = new Log("hallo", new Timestamp(System.currentTimeMillis()), "asdf", Log.Source.STDOUT);
////            log.getStacktrace().add(new Log("stacktrace 1", new Timestamp(System.currentTimeMillis()), "asdf", Log.Source.STDOUT));
////            log.getStacktrace().add(new Log("stacktrace 2", new Timestamp(System.currentTimeMillis()), "asdf", Log.Source.STDOUT));
////
////            Log cause = new Log("cause", new Timestamp(System.currentTimeMillis()), "asdf", Log.Source.STDOUT);
////            cause.getStacktrace().add(new Log("cstack 1", new Timestamp(System.currentTimeMillis()), "asdf", Log.Source.STDOUT));
////            cause.getStacktrace().add(new Log("cstack 2", new Timestamp(System.currentTimeMillis()), "asdf", Log.Source.STDOUT));
////
////            log.setCausedBy(cause);
////
////            session.persist(log);
////            for (Log stack : log.getStacktrace()) {
////                session.persist(stack);
////            }
////
////            session.persist(cause);
////            for (Log stack : cause.getStacktrace()) {
////                session.persist(stack);
////            }
////
////            Log readLog = session.get(Log.class, log.getId());
//
//
//            tx.commit();
//        }

        long timeNano = 1616951864195049000L;
        Timestamp timestamp = new Timestamp(timeNano / 1_000_000);
        timestamp.setNanos((int) (timeNano % 1_000_000_000));

        System.out.println(timestamp);
    }

}
