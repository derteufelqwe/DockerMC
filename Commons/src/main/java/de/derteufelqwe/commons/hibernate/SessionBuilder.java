package de.derteufelqwe.commons.hibernate;

import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.objects.*;
import de.derteufelqwe.commons.hibernate.objects.economy.*;
import de.derteufelqwe.commons.hibernate.objects.permissions.*;
import lombok.Getter;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.Properties;

public class SessionBuilder {

    private String user;
    private String password;
    private String host;
    private int port;

    private SessionFactory sessionFactory;


    public SessionBuilder(String user, String password, String host, int port) {
        this.user = user;
        this.password = password;
        this.host = host;
        this.port = port;

        this.sessionFactory = this.buildSessionFactory();
    }


    public SessionBuilder() {
        this("admin", "password", Constants.POSTGRESDB_CONTAINER_NAME, Constants.POSTGRESDB_PORT);
    }

    
    protected Properties getProperties() {
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
        properties.setProperty(Environment.POOL_SIZE, "1024");

        return properties;
    }

    protected SessionFactory buildSessionFactory() {
        return new Configuration()
                .setProperties(this.getProperties())
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
                .addAnnotatedClass(PlayerToPermissionGroup.class)
                .addAnnotatedClass(Permission.class)
                .addAnnotatedClass(TimedPermission.class)
                .addAnnotatedClass(ServicePermission.class)
                .addAnnotatedClass(Notification.class)
                .addAnnotatedClass(ServiceBalance.class)
                .addAnnotatedClass(PlayerTransaction.class)
                .addAnnotatedClass(ServiceTransaction.class)
                .addAnnotatedClass(Bank.class)
                .addAnnotatedClass(PlayerToBank.class)
                .addAnnotatedClass(BankTransaction.class)
                .buildSessionFactory();
    }

    public Session openSession() {
        return this.sessionFactory.openSession();
    }

    public void close() {
        this.sessionFactory.close();
    }

}
