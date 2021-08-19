package de.derteufelqwe.ServerManager

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Named
import de.derteufelqwe.ServerManager.config.MainConfig
import de.derteufelqwe.ServerManager.config.ServersConfig
import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI
import de.derteufelqwe.ServerManager.utils.Commons
import de.derteufelqwe.commons.Constants
import de.derteufelqwe.commons.config.Config
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter
import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.redis.RedisPool
import picocli.CommandLine.IFactory
import redis.clients.jedis.JedisPool


class GuiceFactory(private val injector: Injector) : IFactory {

    @Throws(Exception::class)
    override fun <K> create(cls: Class<K>): K {
        return injector.getInstance(cls)
    }
}

open class DMCGuiceModule : AbstractModule() {

    @Provides
    @Singleton
    open fun provideMainConfig(): Config<MainConfig> {
        return Config(
            DefaultYamlConverter(),
            DefaultGsonProvider(),
            Constants.CONFIG_PATH + "/main.yml",
            MainConfig()
        )
    }

    @Provides
    @Singleton
    @Named("current")
    open fun provideServerConfig(): Config<ServersConfig> {
        return Config(
            DefaultYamlConverter(),
            DefaultGsonProvider(),
            Constants.CONFIG_PATH + "/servers.yml",
            ServersConfig()
        )
    }

    @Provides
    @Singleton
    @Named("old")
    open fun provideServerConfigOld(): Config<ServersConfig> {
        return Config(
            DefaultYamlConverter(),
            DefaultGsonProvider(),
            Constants.DATA_PATH + "/servers_old.yml",
            ServersConfig()
        )
    }

    @Provides
    @Singleton
    open fun provideDocker(mainConfig: Config<MainConfig>): Docker {
        return Docker("tcp", "ubuntu1", 2375, mainConfig.get())
    }

    @Provides
    @Singleton
    open fun provideSessionBuilder(): SessionBuilder {
        return SessionBuilder(Constants.DB_DMC_USER, "admin", Constants.DMC_MASTER_DNS_NAME)
    }

    @Provides
    @Singleton
    open fun provideRedisPool(): RedisPool {
        return RedisPool("ubuntu1")
    }

    @Provides
    open fun provideJedisPool(redisPool: RedisPool): JedisPool {
        return redisPool.jedisPool
    }

    @Provides
    @Singleton
    open fun provideRegistryApi(mainConfig: Config<MainConfig>): DockerRegistryAPI {
        return DockerRegistryAPI(
            "https://" + Constants.REGISTRY_URL,
            mainConfig.get().registryUsername,
            mainConfig.get().registryPassword
        )
    }

    @Provides
    @Singleton
    open fun provideCommons(mainConfig: Config<MainConfig>, @Named("old")serversConfigOld: Config<ServersConfig>,
                       @Named("current") serversConfig: Config<ServersConfig>, docker: Docker, jedisPool: JedisPool,
                       sessionBuilder: SessionBuilder): Commons {
        return Commons(mainConfig, serversConfig, serversConfigOld, docker, jedisPool, sessionBuilder)
    }

}