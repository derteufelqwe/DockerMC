package de.derteufelqwe.commons.config.providers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

public class TestTest {

    @Test
    public void testTest() {
        ItemStack itemStack = new ItemStack(Material.WOOD_SWORD);
//        itemStack.addEnchantment(Enchantment.DAMAGE_ALL, 2);

        Location location = new Location(Bukkit.getWorld("world"), 1, 2, 3);
    }


}
