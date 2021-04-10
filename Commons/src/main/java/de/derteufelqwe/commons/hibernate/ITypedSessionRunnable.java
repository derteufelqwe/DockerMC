package de.derteufelqwe.commons.hibernate;

import org.hibernate.Session;

@FunctionalInterface
public interface ITypedSessionRunnable<T> {

    T run(Session session);

}
