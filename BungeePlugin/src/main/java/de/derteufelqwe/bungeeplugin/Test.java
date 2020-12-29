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

import javax.imageio.ImageIO;
import java.awt.*;
import java.net.URL;

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

    @SneakyThrows
    public static void main(String[] args) {

//        receive();
        send();

        System.out.println("Done");
    }

}
