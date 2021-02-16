package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class SessionTests {

    public static SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", "ubuntu1", 5432);

    @SneakyThrows
    public static void main(String[] args) {

        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.INFO);
        BuiltConfiguration config = builder.build();

        config.addAppender(ConsoleAppender.createDefaultAppenderForLayout(PatternLayout.createDefaultLayout()));

        LoggerContext context = new LoggerContext("TestLogger");
        context.start(config);

        Logger logger = context.getLogger("TestLogger");

        logger.fatal("Test");
        logger.warn("Test");

    }

}
