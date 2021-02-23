package de.derteufelqwe.bungeeplugin;

import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.protobuf.RedisMessages;
import de.derteufelqwe.commons.redis.RedisPool;
import lombok.SneakyThrows;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.util.List;

public class Test {

    public static JedisPool pool = new RedisPool("ubuntu1").getJedisPool();
//    public static SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", "ubuntu1", 5432);


    public static void receive() {
        try (Jedis jedis = pool.getResource()) {
            System.out.println("Start receive");
            jedis.subscribe(new BinaryJedisPubSub() {
                @SneakyThrows
                @Override
                public void onMessage(byte[] channel, byte[] message) {
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
            } catch (Exception e) {
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


    @SneakyThrows
    public static void main(String[] args) {

        String RED = "\u001B[31m";

        System.out.println(RED + "Hallo");

    }

}
