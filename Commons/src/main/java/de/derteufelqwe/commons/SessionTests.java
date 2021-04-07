package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Log;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class SessionTests {

    public static SessionBuilder sessionBuilder = new SessionBuilder("dockermc", "admin", "ubuntu1", 5432);

    @SneakyThrows
    public static void main(String[] args) {

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

//            DBService service = session.get(DBService.class, "zgm39v057p403mr4ga1jvdqkq");
//            System.out.println(service.getRunningContainersCount());

            System.out.println("############ Start #############");
            List<DBService> services = session.createQuery(
                    "SELECT s FROM DBService s WHERE s.active=true ORDER BY s.active DESC, s.name",
                    DBService.class).getResultList();
//                   List<DBService> services = session.createNativeQuery(
//                    "SELECT s FROM services s WHERE s.active=true ORDER BY s.active DESC, s.name",
//                    DBService.class).getResultList();

            System.out.println(services);

            tx.commit();
        }

    }

}
