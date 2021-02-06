package de.derteufelqwe.commons.hibernate.objects;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import org.hibernate.cfg.Environment;

import javax.persistence.EntityManager;
import java.util.Properties;

public class TestSessionBuilder extends SessionBuilder {

    public TestSessionBuilder() {
        super("", "", "", -1);
    }

    @Override
    protected Properties getProperties() {
        Properties properties = super.getProperties();

        properties.setProperty(Environment.URL, "jdbc:h2:mem:test1");
        properties.setProperty(Environment.DRIVER, "org.h2.Driver");
        properties.setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
        properties.remove(Environment.USER);
        properties.remove(Environment.PASS);

        return properties;
    }

    public Properties getProp() {
        return this.getProperties();
    }

}
