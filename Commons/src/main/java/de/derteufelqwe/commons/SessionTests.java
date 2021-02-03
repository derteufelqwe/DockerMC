package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Notification;
import de.derteufelqwe.commons.hibernate.objects.permissions.Permission;
import de.derteufelqwe.commons.misc.TimeoutMap;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.concurrent.TimeUnit;

public class SessionTests {

    public static SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", "ubuntu1", 5432);

    @SneakyThrows
    public static void main(String[] args) {

        long start = System.nanoTime();

        TimeoutMap<String, String> map = new TimeoutMap<>(1000);
        map.start();

        map.put("a", "1", 20000);
        map.put("b", "2", 200);

        System.out.println(map.get("a"));
        System.out.println(map.get("b"));

        TimeUnit.SECONDS.sleep(2);

        System.out.println(map.get("a"));
        System.out.println(map.get("b"));

//        for (int i = 0; i < 20; i++) {
//            try (Session session = sessionBuilder.openSession()) {
//                Transaction tx = session.beginTransaction();
//
//                DBService service = session.get(DBService.class, "nyhfm6fzpi6qgpka0lam26fjb");
//                DBService service2 = CommonsAPI.getInstance().getActiveServiceFromDB(session, "LobbyServer");
//
//                tx.commit();
//            }
//        }

        long end = System.nanoTime();

        map.interrupt();

        System.out.println("Duration: " + (end - start) / 1000000);
    }

}
