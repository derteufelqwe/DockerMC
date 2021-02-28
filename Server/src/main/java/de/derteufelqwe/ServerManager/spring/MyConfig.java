package de.derteufelqwe.ServerManager.spring;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.config.SystemConfig;
import de.derteufelqwe.commons.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class MyConfig {

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
    public Config<SystemConfig> getSystemConfig() {
        return ServerManager.SYSTEM_CONFIG;
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

}
