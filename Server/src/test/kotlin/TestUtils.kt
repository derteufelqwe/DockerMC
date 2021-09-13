import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Named
import de.derteufelqwe.ServerManager.Docker
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
import de.derteufelqwe.junitDocker.util.RequiredClasses
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.layout.PatternLayout
import redis.clients.jedis.JedisPool
import java.util.*
import kotlin.io.path.createTempDirectory


@RequiredClasses([DMCTestGuiceModule::class, PrintAppender::class])
object TestUtils {

    /**
     * Loads the test config file
     */
    @JvmStatic
    fun getConfig(): TestConfig {
        val props = Properties()
        props.load(this::class.java.getResourceAsStream("testconfig.properties"))

        return TestConfig(
            props.getProperty("DOCKER_HOSTNAME"),
            props.getProperty("DOCKER_PORT").toInt(),
            props.getProperty("IMAGE_TAG_BASE"),
            props.getProperty("IMAGE_TAG_FULL"),
            props.getProperty("IMAGE_TAG_CONFIGURED"),
        )
    }

    /**
     * Create a connection to a docker engine.
     */
    @JvmStatic
    fun getDocker(): DockerClient {
        val config = getConfig()
        return getDocker(config.dockerHostname, config.dockerPort)
    }

    @JvmStatic
    fun getDocker(host: String, port: Int = 2375): DockerClient {
        val clientConfig: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(String.format("%s://%s:%s", "tcp", host, port))
            .withDockerTlsVerify(false)
            .withApiVersion("1.41")
            .build()

        val httpClient: DockerHttpClient = ApacheDockerHttpClient.Builder()
            .dockerHost(clientConfig.dockerHost)
            .sslConfig(clientConfig.sslConfig)
            .build()

        return DockerClientImpl.getInstance(clientConfig, httpClient)
    }

    /**
     * Injects the PrintAppender to the tests logger, which prints the log messages to STDOUT. These logs are then sent
     * to the test runner.
     * This is required as log4j2 doesn't use System.out.println() with its ConsoleAppender. Without this PrintAppender
     * log4j2 logs wouldn't be transferred to the test runner.
     */
    @JvmStatic
    fun injectLogger() {
        val logger = LogManager.getRootLogger() as Logger

        val pa = PrintAppender(
            PatternLayout.newBuilder()
                .withPattern("%d{ISO8601} [%-5level] %msg%n%throwable")
                .build()
        )
        pa.start()
        logger.addAppender(pa)
    }

}

object TestConstants {
    val IMAGE_NAME_BASE = "dockermctest-base"
    val IMAGE_NAME_FULL = "dockermctest-full"
    val IMAGE_NAME_CONFIGURED = "dockermctest-configured"
}

class TestConfig(
    val dockerHostname: String,
    val dockerPort: Int,
    val imageTagBase: String,
    val imageTagFull: String,
    val imageTagConfigured: String,
)

/**
 * A custom version of the DMCGuiceModule, which overrides the providers, which access external resources
 */
class DMCTestGuiceModule(
    val mainConfig: MainConfig = MainConfig(),
    val serversConfig: ServersConfig = ServersConfig(),
    val serversConfigOld: ServersConfig = ServersConfig(),
) : AbstractModule() {

    var folder = createTempDirectory()

    override fun configure() {

    }

    @Provides
    @Singleton
    fun provideMainConfig(): Config<MainConfig> {
        return Config(
            DefaultYamlConverter(),
            DefaultGsonProvider(),
            Constants.CONFIG_PATH + "/main.yml",
            mainConfig
        )
    }

    @Provides
    @Singleton
    @Named("current")
    fun provideServerConfig(): Config<ServersConfig> {
        return Config(
            DefaultYamlConverter(),
            DefaultGsonProvider(),
            Constants.CONFIG_PATH + "/servers.yml",
            serversConfig
        )
    }

    @Provides
    @Singleton
    @Named("old")
    fun provideServerConfigOld(): Config<ServersConfig> {
        return Config(
            DefaultYamlConverter(),
            DefaultGsonProvider(),
            Constants.DATA_PATH + "/servers_old.yml",
            serversConfigOld
        )
    }

    @Provides
    @Singleton
    fun provideDocker(mainConfig: Config<MainConfig>): Docker {
        return Docker("tcp", "localhost", 2375, mainConfig.get())
    }

    @Provides
    @Singleton
    fun provideSessionBuilder(): SessionBuilder {
        return SessionBuilder(Constants.DB_DMC_USER, "admin", "localhost", Constants.POSTGRESDB_PORT)
    }

    @Provides
    @Singleton
    fun provideRedisPool(): RedisPool {
        return RedisPool("localhost")
    }

    @Provides
    fun provideJedisPool(redisPool: RedisPool): JedisPool {
        return redisPool.jedisPool
    }

    @Provides
    @Singleton
    fun provideRegistryApi(mainConfig: Config<MainConfig>): DockerRegistryAPI {
        return DockerRegistryAPI(
            "https://" + Constants.REGISTRY_URL,
            mainConfig.get().registryUsername,
            mainConfig.get().registryPassword
        )
    }

    @Provides
    @Singleton
    fun provideCommons(
        mainConfig: Config<MainConfig>, @Named("old") serversConfigOld: Config<ServersConfig>,
        @Named("current") serversConfig: Config<ServersConfig>, docker: Docker, jedisPool: JedisPool,
        sessionBuilder: SessionBuilder
    ): Commons {
        return Commons(mainConfig, serversConfig, serversConfigOld, docker, jedisPool, sessionBuilder)
    }

}