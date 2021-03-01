package de.derteufelqwe.commons.hibernate;

import de.derteufelqwe.commons.hibernate.objects.Notification;
import org.hibernate.cfg.Environment;

import java.util.Properties;
import java.util.Set;

/**
 * Constructs a session builder, which doesn't required a real DB. The DB used for testing is an in-memory database.
 */
public class TestSessionBuilder extends SessionBuilder {

    private String dbName = "test";


    public TestSessionBuilder() {
        super("", "", "", -1);
    }


    public TestSessionBuilder(String dbName) {
        super("", "", "", 0, false);
        this.dbName = dbName;

        this.init();
    }


    @Override
    public Properties getProperties() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        Properties properties = super.getProperties();

        properties.setProperty(Environment.URL, String.format("jdbc:h2:%s/dmctest/%s;AUTO_SERVER=TRUE", tmpDir, dbName));
        properties.setProperty(Environment.DRIVER, "org.h2.Driver");
        properties.setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
        properties.remove(Environment.USER);
        properties.remove(Environment.PASS);
        properties.setProperty(Environment.HBM2DDL_AUTO, "create");

        return properties;
    }

    @Override
    protected Set<Class<?>> getAnnotatedClasses() {
        Set<Class<?>> annotatedClasses = super.getAnnotatedClasses();

        // Json object not working
        annotatedClasses.remove(Notification.class);

        return annotatedClasses;
    }

    public Properties getProp() {
        return this.getProperties();
    }

}
