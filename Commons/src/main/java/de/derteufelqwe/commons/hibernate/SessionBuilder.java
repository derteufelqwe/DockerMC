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
import org.hibernate.engine.spi.SessionFactoryDelegatingImpl;
import org.hibernate.internal.SessionFactoryImpl;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class SessionBuilder {

    protected String user;
    protected String password;
    protected String host;
    protected int port;

    protected SessionFactory sessionFactory;


    public SessionBuilder(String user, String password, String host, int port, boolean init) {
        this.user = user;
        this.password = password;
        this.host = host;
        this.port = port;

        if (init)
            this.sessionFactory = this.buildSessionFactory();
    }

    public SessionBuilder(String user, String password, String host, int port) {
        this(user, password, host, port, true);
    }

    public SessionBuilder() {
        this("admin", "password", Constants.POSTGRESDB_CONTAINER_NAME, Constants.POSTGRESDB_PORT);
    }


    public void init() {
        this.sessionFactory = this.buildSessionFactory();
    }

    
    public Properties getProperties() {
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

    protected Set<Class<?>> getAnnotatedClasses() {
        Set<Class<?>> annotatedClasses = new HashSet<>();

        annotatedClasses.add(DBContainer.class);
        annotatedClasses.add(Node.class);
        annotatedClasses.add(ContainerStats.class);
        annotatedClasses.add(NodeStats.class);
        annotatedClasses.add(DBService.class);
        annotatedClasses.add(DBPlayer.class);
        annotatedClasses.add(PlayerBan.class);
        annotatedClasses.add(IPBan.class);
        annotatedClasses.add(PlayerLogin.class);
        annotatedClasses.add(PermissionGroup.class);
        annotatedClasses.add(PlayerToPermissionGroup.class);
        annotatedClasses.add(Permission.class);
        annotatedClasses.add(Notification.class);
        annotatedClasses.add(ServiceBalance.class);
        annotatedClasses.add(PlayerTransaction.class);
        annotatedClasses.add(ServiceTransaction.class);
        annotatedClasses.add(Bank.class);
        annotatedClasses.add(PlayerToBank.class);
        annotatedClasses.add(BankTransaction.class);

        return annotatedClasses;
    }

    protected SessionFactory buildSessionFactory() {
        Configuration config = new Configuration()
                .setProperties(this.getProperties());

        for (Class<?> clazz : this.getAnnotatedClasses()) {
            config.addAnnotatedClass(clazz);
        }

        return config.buildSessionFactory();
    }

    public Session openSession() {
        return this.sessionFactory.openSession();
    }

    public void close() {
        this.sessionFactory.close();
    }

}
