package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Log;
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFile;
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder;
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

            VolumeFolder folder = session.get(VolumeFolder.class, 63L);
//            System.out.println(folder.getFiles());

            System.out.println(session.createNativeQuery("select * from volumefiles as f where f.parent_id = :id", VolumeFile.class)
                    .setParameter("id", 63L)
                    .getResultList());


            tx.commit();
        }

    }

}
