package de.derteufelqwe.commons.hibernate;

import de.derteufelqwe.commons.logger.DMCLogger;
import org.hibernate.Session;

/**
 * Base class for runnables, which can be used to run database commands
 */
public abstract class SessionRunnableBase implements Runnable {

    private SessionBuilder sessionBuilder;
    private DMCLogger logger;
    private int retryCounter = 0;
    private int retryInterval = 5;  // In seconds


    public SessionRunnableBase() {
        super();
        sessionBuilder = getSessionBuilder();
        logger = getLogger();
    }

    public SessionRunnableBase(int retryCounter) {
        this();
        this.retryCounter = retryCounter;
    }

    public SessionRunnableBase(int retryCounter, int retryInterval) {
        this(retryCounter);
        this.retryInterval = retryInterval;
    }


    /**
     * Repeatedly executes a runnable until it's completed or out of reruns.
     */
    @Override
    public void run() {
        do {
            try {
                try (Session session = sessionBuilder.openSession()) {
                    this.run(session);
                }
                break;

            } catch (Exception e) {
                if (retryCounter <= 0) {    // Out of retrys
                    throw e;

                } else {
                    logger.warning("Session runnable failed. Retries left: %s.", retryCounter);
                    logger.info("Session runnable failed with: %s.", e.getMessage());
                    logger.fine(e.getStackTrace().toString());
                }

                retryCounter--;
            }

        } while (retryCounter > 0);
    }

    private boolean doRetry() {
        return retryCounter > 0;
    }

    public abstract void run(Session session);

    public abstract SessionBuilder getSessionBuilder();

    public abstract DMCLogger getLogger();

}
