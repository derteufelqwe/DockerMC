package de.derteufelqwe.commons.hibernate;

import de.derteufelqwe.commons.hibernate.objects.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.Properties;

public class SessionBuilder {

    private SessionFactory sessionFactory;


    public SessionBuilder(String user, String password, String host, int port) {
        this.sessionFactory = new Configuration()
            .setProperties(this.getProperties(user, password, host, port))
            .addAnnotatedClass(DBContainer.class)
            .addAnnotatedClass(Node.class)
            .addAnnotatedClass(ContainerStats.class)
            .addAnnotatedClass(NodeStats.class)
            .addAnnotatedClass(DBService.class)
            .addAnnotatedClass(DBPlayer.class)
            .addAnnotatedClass(PlayerOnlineDurations.class)
            .addAnnotatedClass(PlayerBan.class)
            .addAnnotatedClass(PlayerLogin.class)
            .addAnnotatedClass(IPBan.class)
            .addAnnotatedClass(PermissionGroup.class)
            .addAnnotatedClass(PlayerPermissionGroup.class)
            .addAnnotatedClass(Permission.class)
            .addAnnotatedClass(TimedPermission.class)
            .buildSessionFactory();
    }
    
    private Properties getProperties(String user, String password, String host, int port) {
        Properties properties = new Properties();

        properties.setProperty(Environment.DRIVER, "org.postgresql.Driver");
        properties.setProperty(Environment.URL, "jdbc:postgresql://" + host + ":" + port + "/postgres?useSSL=false");
        properties.setProperty(Environment.USER, user);
        properties.setProperty(Environment.PASS, password);
        properties.setProperty(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty(Environment.SHOW_SQL, "false");
        properties.setProperty(Environment.HBM2DDL_AUTO, "update"); // create / update
        properties.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        properties.setProperty(Environment.PHYSICAL_NAMING_STRATEGY, "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
        properties.setProperty(Environment.POOL_SIZE, "64");

        return properties;
    }

    public Session openSession() {
        return this.sessionFactory.openSession();
    }

    public void close() {
        this.sessionFactory.close();
    }

}
