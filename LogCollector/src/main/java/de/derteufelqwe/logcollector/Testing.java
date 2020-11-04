package de.derteufelqwe.logcollector;

import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.Properties;

public class Testing {

    @SneakyThrows
    public static void main(String[] args) {

        Properties properties = new Properties();
        properties.setProperty(Environment.DRIVER, "org.postgresql.Driver");
        properties.setProperty(Environment.URL, "jdbc:postgresql://ubuntu1:5432/postgres?useSSL=false");
        properties.setProperty(Environment.USER, "admin");
        properties.setProperty(Environment.PASS, "password");
        properties.setProperty(Environment.DIALECT, "org.hibernate.dialect.PostgreSQL95Dialect");
        properties.setProperty(Environment.SHOW_SQL, "false");
        properties.setProperty(Environment.HBM2DDL_AUTO, "create-drop"); // create-drop, update
        properties.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");

        SessionFactory sessionFactory = new Configuration()
                .setProperties(properties)
                .addAnnotatedClass(Container.class)
                .buildSessionFactory();

        Session session = sessionFactory.openSession();

        System.out.println("-----  Connected to database  -----");

        Transaction t = session.beginTransaction();

        Container container = new Container("idasd", "Ich bin ein Log");
        session.persist(container);

        Container c = session.get(Container.class, "idasd");

        t.commit();

        System.out.println("-----  Closing connection  -----");

        session.clear();
        session.close();
        sessionFactory.close();
    }

}
