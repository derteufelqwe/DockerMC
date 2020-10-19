package de.derteufelqwe.commons.config.providers;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SerializationTests {

    private ServerMock server;
    private YamlConverter converter;
    private Gson gson;

    @BeforeAll
    public void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        server.addPlayer(new PlayerMock(server, "TestPlayer", new UUID(-100L, 100L)));

        converter = new DefaultYamlConverter(new DefaultYamlProvider(), new MinecraftGsonProvider());
        gson = new MinecraftGsonProvider().getGson();
    }


    @Test
    public void testWorld() {
        World world = Bukkit.getWorld("world");

        String data = converter.dumpJson(world);
        System.out.println(data);

        assertEquals(data.trim(),  world.getUID().toString().trim());

        World newWorld = gson.fromJson(converter.loadJson(data), World.class);

        assertEquals(world.getUID(), newWorld.getUID());
        assertEquals(world.getName(), newWorld.getName());
        assertEquals(world, newWorld);
    }

    @Test
    public void testLocation() {
        Location location = new Location(Bukkit.getWorld("world"), 1, 2, 3);

        String data = converter.dumpJson(location);
        System.out.println(data);

        Location newLocation = gson.fromJson(converter.loadJson(data), Location.class);
        System.out.println(newLocation);

        assertEquals(location, newLocation);
    }

    @Test
    public void testPlayer() {
        Player player = server.getPlayer("TestPlayer");

        String data = converter.dumpJson(gson.toJsonTree(player));
        System.out.println(data);

        Player newPlayer = gson.fromJson(converter.loadJson(data), Player.class);

        assertEquals(player, newPlayer);
    }

    @Test
    public void testSerialization() {
        LocationContainer container = new LocationContainer();

        String data = this.converter.dumpJson(gson.toJsonTree(container));

        System.out.println(data);

        LocationContainer newContainer = gson.fromJson(this.converter.loadJson(data), LocationContainer.class);

        assertEquals(container, newContainer);
    }



    @AfterAll
    public void tearDown() {
        MockBukkit.unload();
    }

}