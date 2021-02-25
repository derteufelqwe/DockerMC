package de.derteufelqwe.commons.hibernate.objects;

import de.derteufelqwe.commons.hibernate.TestSessionBuilder;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.*;
import java.util.Properties;
import java.util.UUID;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestTest {

    private EntityManagerFactory factory;

    private TestSessionBuilder sessionBuilder;

    private int COUNTER = 20000;

    private UUID playerId = UUID.randomUUID();


    @BeforeAll
    public void begin() {
        Properties prop = new TestSessionBuilder().getProp();
        factory = Persistence.createEntityManagerFactory("Test1", prop);
        sessionBuilder = new TestSessionBuilder();

        EntityManager manager = factory.createEntityManager();
        manager.getTransaction().begin();
        DBPlayer player = new DBPlayer(playerId, "Arne");
        manager.persist(player);
        manager.getTransaction().commit();
        manager.close();
    }


    @Test
    public void testManagerPerformance() {

        long start = System.currentTimeMillis();

        for (int i = 0; i < COUNTER; i++) {
            EntityManager m = factory.createEntityManager();
            m.createNativeQuery("SELECT * FROM players WHERE name = 'Arne'", DBPlayer.class).getSingleResult();
            m.close();
        }

        System.out.println("Manager: " + (System.currentTimeMillis() - start) + " ms.");
    }


    @Test
    public void testSessionPerformance() {
        long start = System.currentTimeMillis();

        for (int i = 0; i < COUNTER; i++) {
            try (Session session = sessionBuilder.openSession()) {
                session.createNativeQuery("SELECT * FROM players WHERE name = 'Arne'", DBPlayer.class).getSingleResult();
//                session.get(DBPlayer.class, playerId);
            }
        }

        System.out.println("Session: " + (System.currentTimeMillis() - start) + " ms.");

    }

}
