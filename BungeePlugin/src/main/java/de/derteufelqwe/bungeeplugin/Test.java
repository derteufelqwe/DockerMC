package de.derteufelqwe.bungeeplugin;

import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import de.derteufelqwe.commons.redis.RedisPool;
import de.derteufelqwe.commons.protobuf.RedisMessages;
import lombok.SneakyThrows;
import org.checkerframework.checker.units.qual.C;
import redis.clients.jedis.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Test {

    public static JedisPool pool = new RedisPool("ubuntu1").getJedisPool();
//    public static SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", "ubuntu1", 5432);


    public static void receive() {
        try (Jedis jedis = pool.getResource()) {
            System.out.println("Start receive");
            jedis.subscribe(new BinaryJedisPubSub() {
                @SneakyThrows
                @Override
                public void onMessage(byte[] channel, byte[] message){
                    RedisMessages.RedisMessage msg = RedisMessages.RedisMessage.parseFrom(message);
                    System.out.println("got message");
                }
            }, Constants.REDIS_MESSAGES_CHANNEL);
        }
    }


    public static void send() {
        try (Jedis jedis = pool.getResource()) {
            System.out.println("Start send");
            RedisMessages.RedisMessage msg = RedisMessages.RedisMessage.newBuilder()
                    .setPlayerChangeServer(RedisMessages.PlayerChangeServer.newBuilder()
                            .setOldServer("sdf")
                            .build())
                    .build();

            jedis.publish(Constants.REDIS_MESSAGES_CHANNEL, msg.toByteArray());
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


        Config<ConfigFile> config = new Config<>(new DefaultYamlConverter(), new DefaultGsonProvider(), "test.yml", new ConfigFile());
        config.load();


        config.save();
    }

}
