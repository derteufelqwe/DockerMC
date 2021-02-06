package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Notification;
import de.derteufelqwe.commons.hibernate.objects.permissions.Permission;
import de.derteufelqwe.commons.misc.TimeoutMap;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.EntityManager;
import java.util.concurrent.TimeUnit;

public class SessionTests {

    public static SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", "ubuntu1", 5432);

    @SneakyThrows
    public static void main(String[] args) {

        long start = System.nanoTime();


        for (int i = 0; i < 10000; i++) {
//            try (Session session = sessionBuilder.openSession()) {
////                DBService service = session.get(DBService.class, "nyhfm6fzpi6qgpka0lam26fjb");
//                session.createNativeQuery("select * from services as s where s.name = 'LobbyServer'", DBService.class).getSingleResult();
//            }
        }

        long end = System.nanoTime();


        System.out.println("Duration: " + (end - start) / 1000000);
    }

}
