package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Log;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class SessionTests {

    public static SessionBuilder sessionBuilder = new SessionBuilder("dockermc", "admin", "ubuntu1", 5432);

    @SneakyThrows
    public static void main(String[] args) {

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            DBService service = session.get(DBService.class, "jezvcpjp038akplwsggeo92mi");


            tx.commit();
        }

    }

}
