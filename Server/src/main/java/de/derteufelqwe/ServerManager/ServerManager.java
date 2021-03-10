package de.derteufelqwe.ServerManager;

import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.config.OldServersConfig;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.extern.log4j.Log4j2;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.service.spi.ServiceException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;

import java.net.ConnectException;


@SpringBootApplication
@Log4j2
public class ServerManager {

    /*
     * ToDo-List:
     *  - Docker secrets for password management
     *  - Improved config system
     *  - Clean database layouts
     *  - Switch overlay network
     */

    public static final boolean SKIP_STARTUP_CHECKS = true;

    /*
     * Exit codes:
     *  100: Infrastructure setup failed
     *  101: Minecraft server setup failed
     *  102: Invalid server config file
     */

    public static final Config<MainConfig> MAIN_CONFIG = new Config<>(new DefaultYamlConverter(), new DefaultGsonProvider(), Constants.CONFIG_PATH + "/main.yml", new MainConfig());
    public static final Config<OldServersConfig> SERVERS_CONFIG_OLD = new Config<>(new DefaultYamlConverter(), new DefaultGsonProvider(), Constants.DATA_PATH + "/servers_old.yml", new OldServersConfig());
    public static final Config<ServersConfig> SERVERS_CONFIG = new Config<>(new DefaultYamlConverter(), new DefaultGsonProvider(), Constants.CONFIG_PATH + "/servers.yml", new ServersConfig());

    static {
        MAIN_CONFIG.load();
        SERVERS_CONFIG_OLD.load();
        SERVERS_CONFIG.load();
    }


    public static void main(String[] args) {
        SpringApplication.run(ServerManager.class, args);
    }

}
