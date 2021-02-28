package de.derteufelqwe.ServerManager;

import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.config.SystemConfig;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServerManager {

    /*
     * Exit codes:
     *  100: Infrastructure setup failed
     *  101: Minecraft server setup failed
     *  102: Invalid server config file
     */

    public static final Config<MainConfig> MAIN_CONFIG = new Config<>(new DefaultYamlConverter(), new DefaultGsonProvider(), Constants.CONFIG_PATH + "/main.yml", new MainConfig());
    public static final Config<SystemConfig> SYSTEM_CONFIG = new Config<>(new DefaultYamlConverter(), new DefaultGsonProvider(), Constants.CONFIG_PATH + "/system.yml", new SystemConfig());
    public static final Config<ServersConfig> SERVERS_CONFIG = new Config<>(new DefaultYamlConverter(), new DefaultGsonProvider(), Constants.CONFIG_PATH + "/servers.yml", new ServersConfig());

    static {
        MAIN_CONFIG.load();
        SYSTEM_CONFIG.load();
        SERVERS_CONFIG.load();
    }


    public static void main(String[] args) {
        SpringApplication.run(ServerManager.class, args);
    }

}
