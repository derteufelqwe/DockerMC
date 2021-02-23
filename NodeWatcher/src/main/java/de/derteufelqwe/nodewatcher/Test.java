package de.derteufelqwe.nodewatcher;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {


    public static void main(String[] args) {
        Logger logger = LogManager.getLogger("TestLogger");

        logger.log(Level.ALL, "Log");
        logger.debug("Debug");
        logger.trace("Trace");
        logger.info("Info");
        logger.warn("Warning");
        logger.error("Error");
        logger.fatal("fatal");
    }

}
