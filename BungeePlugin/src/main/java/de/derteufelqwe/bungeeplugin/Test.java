package de.derteufelqwe.bungeeplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.derteufelqwe.bungeeplugin.redis.RedisHandler;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.MojangAPIProfile;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.MojangAPIProfileDeserializer;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.PlayerTextureDeserializer;
import lombok.SneakyThrows;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Transaction;

import javax.imageio.ImageIO;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Test {

    public static JedisPool pool = new RedisHandler("ubuntu1").getJedisPool();


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


    @SneakyThrows
    public static void main(String[] args) {

//        receive();
//        send();
        multiBlock();
//        multiSet();

        System.out.println("Done");
    }

}
