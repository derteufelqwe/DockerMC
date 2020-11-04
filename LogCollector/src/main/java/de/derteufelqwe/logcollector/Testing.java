package de.derteufelqwe.logcollector;

import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Testing {

    @SneakyThrows
    public static void main(String[] args) {
        Logger.getLogger("org.hibernate").setLevel(Level.WARNING);

        SessionBuilder sessionBuilder = new SessionBuilder();

        Session session = sessionBuilder.openSession();

        Transaction t = session.beginTransaction();

//        Container container = new Container("idasd");
//        session.persist(container);


        t.commit();

        session.clear();
        session.close();
        sessionBuilder.close();
    }

}
