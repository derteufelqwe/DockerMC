package de.derteufelqwe.commons.hibernate;

import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * A utility class that makes executing DB stuff easier.
 */
public abstract class TypedSessionRunnable<T> {

    private SessionBuilder sessionBuilder;


    public TypedSessionRunnable(SessionBuilder sessionBuilder) {
        this.sessionBuilder = sessionBuilder;
    }

    /**
     * Runs the exec method in a transaction
     */
    public T run() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                T retVal = exec(session);
                tx.commit();
                return retVal;

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }

        }
    }

    protected abstract T exec(Session session);

}
