package serialization;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.google.gson.Gson;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import de.derteufelqwe.commons.config.providers.DefaultYamlProvider;
import de.derteufelqwe.commons.config.providers.YamlConverter;
import de.derteufelqwe.minecraftplugin.config.providers.MinecraftGsonProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

    @BeforeClass
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

        assertEquals(data.trim(), world.getUID().toString().trim());

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

    @Test
    public void testTesting() {

    }

    @AfterClass
    public void tearDown() {
        MockBukkit.unload();
    }

}