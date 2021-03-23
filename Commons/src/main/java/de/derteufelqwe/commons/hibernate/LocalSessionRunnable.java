package de.derteufelqwe.commons.hibernate;

import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * A utility class that makes executing DB stuff easier.
 */
public abstract class LocalSessionRunnable {

    private SessionBuilder sessionBuilder;


    public LocalSessionRunnable(SessionBuilder sessionBuilder) {
        this.sessionBuilder = sessionBuilder;
    }

    /**
     * Runs the exec method in a transaction
     */
    public void run() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                exec(session);
                tx.commit();

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }

        }
    }

    protected abstract void exec(Session session);

}
