package de.derteufelqwe.commons.hibernate;

import de.derteufelqwe.commons.hibernate.objects.Container;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.Properties;

public class SessionBuilder {

    private SessionFactory sessionFactory;


    public SessionBuilder(String user, String password, String url, boolean dropTable) {
        this.sessionFactory = new Configuration()
            .setProperties(this.getProperties(user, password, url, dropTable))
             .addAnnotatedClass(Container.class)
             .buildSessionFactory();
    }
    
    private Properties getProperties(String user, String password, String url, boolean dropTable) {
        Properties properties = new Properties();

        String hbm2ddlType = "update";
        if (dropTable) {
            hbm2ddlType = "create";
        }

        properties.setProperty(Environment.DRIVER, "org.postgresql.Driver");
        properties.setProperty(Environment.URL, "jdbc:postgresql://" + url + "/postgres?useSSL=false");
        properties.setProperty(Environment.USER, user);
        properties.setProperty(Environment.PASS, password);
        properties.setProperty(Environment.DIALECT, "org.hibernate.dialect.PostgreSQL95Dialect");
        properties.setProperty(Environment.SHOW_SQL, "false");
        properties.setProperty(Environment.HBM2DDL_AUTO, hbm2ddlType); // create / update
        properties.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");

        return properties;
    }

    public Session openSession() {
        return this.sessionFactory.openSession();
    }

    public void close() {
        this.sessionFactory.close();
    }

}