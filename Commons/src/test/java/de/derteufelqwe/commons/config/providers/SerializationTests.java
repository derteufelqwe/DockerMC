package de.derteufelqwe.commons.config.providers;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.block.BlockMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.inventory.meta.BookMetaMock;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

    private String serialize(Object obj) {
        return this.converter.dumpJson(obj);
    }

    private <T> T deserialize(String data, Class<?> clazz) {
        return (T) this.gson.fromJson(this.converter.loadJson(data), clazz);
    }

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

        String data = this.serialize(world);
        System.out.println(data);

        assertEquals(data.trim(),  world.getUID().toString().trim());

        World newWorld = this.deserialize(data, World.class);

        assertEquals(world.getUID(), newWorld.getUID());
        assertEquals(world.getName(), newWorld.getName());
        assertEquals(world, newWorld);
    }

    @Test
    public void testLocation() {
        Location location = new Location(Bukkit.getWorld("world"), 1, 2, 3);

        String data = this.serialize(location);
        System.out.println(data);

        Location newLocation = this.deserialize(data, Location.class);
        System.out.println(newLocation);

        assertEquals(location, newLocation);
    }

    @Test
    public void testPlayer() {
        Player player = server.getPlayer("TestPlayer");

        String data = this.serialize(player);
        System.out.println(data);

        Player newPlayer = this.deserialize(data, Player.class);

        assertEquals(player, newPlayer);
    }

    @Test
    public void testCustomObject() {
        LocationContainer container = new LocationContainer();

        String data = this.serialize(container);
        System.out.println(data);

        LocationContainer newContainer = this.deserialize(data, LocationContainer.class);

        assertEquals(container, newContainer);
    }

    @Test
    public void testItemStacks() {
        ItemStack itemStack = new ItemStack(Material.SIGN);

        String data = this.serialize(itemStack);
        System.out.println(data);

//        ItemStack newItemStack = this.deserialize(data, ItemStack.class);
//        assertEquals(itemStack, newItemStack);
    }


    @AfterAll
    public void tearDown() {
        MockBukkit.unload();
    }

}