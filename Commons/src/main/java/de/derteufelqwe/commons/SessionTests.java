package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Permission;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class SessionTests {

    public static SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", "ubuntu1", 5432);

    public static void main(String[] args) {

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            Permission permission1 = session.get(Permission.class, 1L);
            Permission permission2 = session.get(Permission.class, 2L);

            tx.commit();
        }

    }

}
