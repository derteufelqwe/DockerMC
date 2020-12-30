package de.derteufelqwe.bungeeplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.derteufelqwe.bungeeplugin.redis.RedisHandler;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.MojangAPIProfile;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.MojangAPIProfileDeserializer;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.PlayerTextureDeserializer;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import lombok.SneakyThrows;
import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Transaction;

import javax.imageio.ImageIO;
import java.awt.*;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

public class Test {

    public static JedisPool pool = new RedisHandler("ubuntu1").getJedisPool();
//    public static SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", "ubuntu1", 5432);


    public static void receive() {
        try (Jedis jedis = pool.getResource()) {
            System.out.println("Start receive");
            jedis.psubscribe(new JedisPubSub() {
                @Override
                public void onPMessage(String pattern, String channel, String message) {
                    String time = message.split("-")[1];
                    long lTime = Long.parseLong(time);

                    System.out.println("Delta: " + (System.currentTimeMillis() - lTime));
                }
            }, "test#*");        }
    }


    public static void send() {

    for (int i = 0; i < 10000; i++) {
        try (Jedis jedis = pool.getResource()) {
            System.out.println("Start send");
                jedis.publish("test#msg", i + "-" + System.currentTimeMillis());
            }
        }

    }

    public static void multiBlock() {
        try (Jedis jedis = pool.getResource()) {
            Transaction tx = jedis.multi();

            try {
                tx.hgetAll("players#derteufelqwe");

                List<Object> res = tx.exec();
                System.out.println("Read");
            } catch (Exception e){
                tx.clear();
                tx.close();
                throw e;
            }

        }
    }

    public static void multiSet() {
        System.out.println("set 1");
        try (Jedis jedis = pool.getResource()) {
            System.out.println("set 2");
            jedis.set("value", Long.toString(System.currentTimeMillis() / 1000L));
            System.out.println("set 3");
        }
    }

//    public static void readDb() {
//        DBPlayer dbPlayer = null;
//
//        try (Session session = sessionBuilder.openSession()) {
//            UUID uuid = UUID.fromString("81875c3d-f697-3ff2-806c-cb0b547af83e");
//
////            dbPlayer = session.get(DBPlayer.class, uuid);
//
//            dbPlayer = (DBPlayer) session.createCriteria(DBPlayer.class)
////                    .setFetchMode("gottenBans", FetchMode.JOIN)
//                    .add(Restrictions.idEq(uuid))
//                    .uniqueResult();
//        }
//
//        System.out.println(dbPlayer.getGottenBans());
//    }

    @SneakyThrows
    public static void main(String[] args) {

//        receive();
//        send();
//        multiBlock();
//        multiSet();
//        readDb();

        Duration d = Duration.of(4300010, ChronoUnit.SECONDS);


        long tmp = d.getSeconds();
        long seconds = tmp % 60;
        tmp = tmp / 60;
        long minutes = tmp % 60;
        tmp = tmp / 60;
        long hours = tmp % 24;
        tmp = tmp / 60;
        long days = tmp;

        System.out.println(String.format(
                "%d days %d:%02d:%02d",
            days, hours, minutes, seconds
        ));


        System.out.println("Done");
    }

}
