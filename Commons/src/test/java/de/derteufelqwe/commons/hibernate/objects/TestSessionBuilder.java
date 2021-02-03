package de.derteufelqwe.commons.hibernate.objects;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import org.hibernate.cfg.Environment;

import java.util.Properties;

public class TestSessionBuilder extends SessionBuilder {

    public TestSessionBuilder(String user, String password, String host, int port) {
        super(user, password, host, port);
    }

    @Override
    protected Properties getProperties(String user, String password, String host, int port) {
        Properties properties = super.getProperties(user, password, host, port);

        properties.setProperty(Environment.URL, "jdbc:h2:mem:test1");
        properties.setProperty(Environment.DRIVER, "org.h2.Driver");
        properties.setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
        properties.remove(Environment.USER);
        properties.remove(Environment.PASS);

        return properties;
    }
}
