package de.derteufelqwe.ServerManager.spring;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.config.OldServersConfig;
import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.service.spi.ServiceException;
import org.jline.terminal.Terminal;
import org.postgresql.Driver;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@Log4j2
public class MyConfig {

    @Getter
    private static SessionBuilder sessionBuilder;

    @Autowired
    private Config<MainConfig> mainConfig;


    @Bean
    public Docker getDocker() {
        return new Docker("tcp", "ubuntu1", 2375, mainConfig.get());
    }

    @Bean
    public Config<MainConfig> getMainConfig() {
        return ServerManager.MAIN_CONFIG;
    }

    @Bean
    public Config<OldServersConfig> getSystemConfig() {
        return ServerManager.SERVERS_CONFIG_OLD;
    }

    @Bean
    public Config<ServersConfig> getServersConfig() {
        return ServerManager.SERVERS_CONFIG;
    }

    @Bean
    public JedisConnectionFactory getRedisFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("ubuntu1", 6379);

        return new JedisConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> getRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(this.getRedisFactory());

        return template;
    }

    @Bean
    SessionBuilder sessionBuilderBean() {
        sessionBuilder = new SessionBuilder("admin", "password", "ubuntu1", Constants.POSTGRESDB_PORT, false);

        try {
            sessionBuilder.init();

        } catch (Exception e) {
            log.warn("Couldn't connect to postgres database. Maybe it's just not started yet.");
        }

        return sessionBuilder;
    }

    @Bean
    public DockerRegistryAPI dockerRegistryAPI() {
        return new DockerRegistryAPI("https://" + Constants.REGISTRY_URL, mainConfig.get().getRegistryUsername(), mainConfig.get().getRegistryPassword());
    }

}
