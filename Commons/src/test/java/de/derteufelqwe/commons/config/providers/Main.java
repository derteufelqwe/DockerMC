package de.derteufelqwe.commons.config.providers;

import de.derteufelqwe.commons.config.Config;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        Config config = new Config(new DefaultYamlConverter(), new DefaultGsonProvider());
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
