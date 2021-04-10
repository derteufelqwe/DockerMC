package de.derteufelqwe.commons.hibernate;

import org.hibernate.Session;

public interface ISessionRunnable {

    void run(Session session);

}
