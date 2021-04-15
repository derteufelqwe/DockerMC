package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Log;
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume;
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFile;
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class SessionTests {

    public static SessionBuilder sessionBuilder = new SessionBuilder("dockermc", "admin", "ubuntu1", 5432);

    @SneakyThrows
    public static void main(String[] args) {

        String id = UUID.randomUUID().toString();

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();
            System.out.println("##################  Start  ##################");

            Volume volume = new Volume(id, new Timestamp(System.currentTimeMillis()));

            VolumeFolder folder = new VolumeFolder("/");
            folder.setVolume(volume);

            session.persist(folder);
            session.persist(volume);

            tx.commit();
        }

        sessionBuilder.execute(session -> {
            Volume volume = session.get(Volume.class, id);
            System.out.println(volume);
        });

    }

}
