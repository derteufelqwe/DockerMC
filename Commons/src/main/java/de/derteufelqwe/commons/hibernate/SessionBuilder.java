package de.derteufelqwe.commons.hibernate;

import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.exceptions.DockerMCException;
import de.derteufelqwe.commons.hibernate.objects.*;
import de.derteufelqwe.commons.hibernate.objects.economy.*;
import de.derteufelqwe.commons.hibernate.objects.permissions.Permission;
import de.derteufelqwe.commons.hibernate.objects.permissions.PermissionGroup;
import de.derteufelqwe.commons.hibernate.objects.permissions.PlayerToPermissionGroup;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.mapping.MetadataSource;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class SessionBuilder {

    protected String user;
    protected String password;
    protected String host;
    protected int port;

    public SessionFactory sessionFactory;


    public SessionBuilder(String user, String password, String host, int port, boolean init) {
        this.user = user;
        this.password = password;
        this.host = host;
        this.port = port;

        if (init)
            this.init();
    }

    public SessionBuilder(String user, String password, String host, int port) {
        this(user, password, host, port, true);
    }

    public SessionBuilder(String user, String password, String host) {
        this(user, password, host, Constants.POSTGRESDB_PORT);
    }

    public SessionBuilder(String password, String host) {
        this(Constants.DB_DMC_USER, password, host);
    }

    public SessionBuilder(String password) {
        this(password, Constants.DMC_MASTER_DNS_NAME);
    }


    public void init() throws JDBCConnectionException {
        if (sessionFactory != null)
            sessionFactory.close();

        this.sessionFactory = this.buildSessionFactory();
    }


    public Properties getProperties() {
        Properties properties = new Properties();

        properties.setProperty(Environment.DRIVER, "org.postgresql.Driver");
        properties.setProperty(Environment.URL, "jdbc:postgresql://" + host + ":" + port + "/dockermc");
        properties.setProperty(Environment.USER, user);
        properties.setProperty(Environment.PASS, password);
        properties.setProperty(Environment.DIALECT, "org.hibernate.dialect.PostgreSQL10Dialect");
        properties.setProperty(Environment.SHOW_SQL, "false");
        properties.setProperty(Environment.HBM2DDL_AUTO, "update"); // create / update
        properties.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        properties.setProperty(Environment.PHYSICAL_NAMING_STRATEGY, "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
        properties.setProperty(Environment.POOL_SIZE, "1024");

        // --- Connection pool ---
        properties.setProperty("hibernate.c3p0.min_size", "5");
        properties.setProperty("hibernate.c3p0.max_size", "20");
        properties.setProperty("hibernate.c3p0.timeout", "300");
        properties.setProperty("hibernate.c3p0.max_statements", "50");
        // Check connection integrity
        properties.setProperty("hibernate.c3p0.idleConnectionTestPeriod", "10");    // in seconds
        properties.setProperty("hibernate.c3p0.testConnectionOnCheckin", "true");
        properties.setProperty("hibernate.c3p0.testConnectionOnCheckout", "true");
        properties.setProperty("hibernate.c3p0.preferredTestQuery", "SELECT 1");

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
        annotatedClasses.add(DBContainerHealth.class);
        annotatedClasses.add(Log.class);
        annotatedClasses.add(DBServiceHealth.class);

        return annotatedClasses;
    }

    protected SessionFactory buildSessionFactory() {
        Configuration config = new Configuration()
                .setProperties(this.getProperties());

        for (Class<?> clazz : this.getAnnotatedClasses()) {
            config.addAnnotatedClass(clazz);
        }

        try {
            return config.buildSessionFactory();

        } catch (NullPointerException e) {
            throw new DockerMCException(e);
        }
    }

    public Session openSession() {
        if (sessionFactory == null)
            this.init();

        return this.sessionFactory.openSession();
    }

    public void close() {
        if (this.sessionFactory != null) {
            this.sessionFactory.close();
        }
    }

}
