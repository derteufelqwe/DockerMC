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
import org.hibernate.cfg.Environment;
import org.postgresql.Driver;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public LocalSessionFactoryBean sessionFactoryBean() {
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(this.dataSource());
        sessionFactoryBean.setPackagesToScan("de.derteufelqwe.commons.hibernate.objects");
        sessionFactoryBean.setHibernateProperties(this.hibernateProperties());

        return sessionFactoryBean;
    }

    @Bean
    public DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerName("ubuntu1");
        dataSource.setDatabaseName("postgres");
        dataSource.setUser("admin");
        dataSource.setPassword("password");

        return dataSource;
    }

    private final Properties hibernateProperties() {
        Properties properties = new Properties();

        properties.setProperty(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty(Environment.SHOW_SQL, "false");
        properties.setProperty(Environment.HBM2DDL_AUTO, "update"); // create / update
        properties.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        properties.setProperty(Environment.PHYSICAL_NAMING_STRATEGY, "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
        properties.setProperty(Environment.POOL_SIZE, "32");

        return properties;
    }

    @Bean
    public PlatformTransactionManager hibernateTransactionManager() {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(this.sessionFactoryBean().getObject());

        return transactionManager;
    }

    @Bean
    public DockerRegistryAPI dockerRegistryAPI() {
        return null;
//        return new DockerRegistryAPI("https://" + Constants.REGISTRY_URL, mainConfig.get().getRegistryUsername(), mainConfig.get().getRegistryPassword());
    }

}
