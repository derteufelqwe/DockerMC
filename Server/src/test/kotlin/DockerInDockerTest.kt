import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Named
import de.derteufelqwe.ServerManager.DMCGuiceModule
import de.derteufelqwe.ServerManager.Docker
import de.derteufelqwe.ServerManager.GuiceFactory
import de.derteufelqwe.ServerManager.cli.CliCommands
import de.derteufelqwe.ServerManager.cli.converters.DurationConverter
import de.derteufelqwe.ServerManager.config.MainConfig
import de.derteufelqwe.ServerManager.config.ServersConfig
import de.derteufelqwe.ServerManager.config.objects.CertificateCfg
import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI
import de.derteufelqwe.ServerManager.setup.servers.BungeePool
import de.derteufelqwe.ServerManager.utils.Commons
import de.derteufelqwe.commons.Constants
import de.derteufelqwe.commons.config.Config
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter
import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.redis.RedisPool
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands
import redis.clients.jedis.JedisPool
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import kotlin.io.path.createTempDirectory


private val IMAGE_NAME = "dockermctest-full:latest"
private val HOSTNAME = "ubuntu1"


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DockerInDockerTest {

    private lateinit var localDocker: DockerClient
    private lateinit var docker: DockerClient
    private lateinit var containerID: String
    private var dockerPort = -1
    private var postgresPort = -1


    @BeforeAll
    fun startUp() {
        this.localDocker = getDocker(HOSTNAME, 2375)

        // Create the docker container to run the tests in
        val container = localDocker.createContainerCmd(IMAGE_NAME)
            .withHostConfig(HostConfig()
                .withRuntime("sysbox-runc")
                .withPortBindings(
                    PortBinding(Ports.Binding.empty(), ExposedPort.tcp(2375)),
                    PortBinding(Ports.Binding.empty(), ExposedPort.tcp(5432))
                ))
            .exec()

        localDocker.startContainerCmd(container.id).exec()
        containerID = container.id

        // Connect to the test container
        val inspectResp = localDocker.inspectContainerCmd(containerID).exec()
        dockerPort = inspectResp.networkSettings.ports.bindings[ExposedPort.tcp(2375)]!![0].hostPortSpec.toInt()
        postgresPort = inspectResp.networkSettings.ports.bindings[ExposedPort.tcp(5432)]!![0].hostPortSpec.toInt()

        docker = getDocker(HOSTNAME, dockerPort)
        docker.pingCmd().exec()

        println("Connected to docker engine in '$containerID:$dockerPort'.")
    }

    @AfterAll
    fun tearDown() {

        docker.close()
        println("Disconnected from docker engine in '$containerID:$dockerPort'.")

        localDocker.killContainerCmd(containerID).exec()
        localDocker.close()
    }


    @Test
    fun testDocker() {
        val injector = Guice.createInjector(DMCTestGuiceModule(dockerPort = dockerPort, postgresPort = postgresPort))
//        // Set up picocli commands
        val commands = CliCommands()
        val factory = PicocliCommands.PicocliCommandsFactory(GuiceFactory(injector))
        val cmd = CommandLine(commands, factory)
        cmd.registerConverter(Duration::class.java, DurationConverter())

        val sw = StringWriter()
        cmd.out = PrintWriter(sw)

//        val exitCode = cmd.execute("system", "listNodes")
        val exitCode = cmd.execute("system", "createInfrastructure", "-a")
        println("Done")
    }

    @Test
    fun testSetupSystem() {
        val mainConfig = MainConfig()
        mainConfig.registryCerCfg.countryCode = "DE"
        mainConfig.registryCerCfg.state = "BE"
        mainConfig.registryCerCfg.city = "City"
        mainConfig.registryCerCfg.organizationName = "TestOrg"
        mainConfig.registryCerCfg.email = "test@test.de"

        val injector = Guice.createInjector(DMCTestGuiceModule(dockerPort = dockerPort, postgresPort = postgresPort, mainConfig = mainConfig))
        val commands = CliCommands()
        val factory = PicocliCommands.PicocliCommandsFactory(GuiceFactory(injector))
        val cmd = CommandLine(commands, factory)
        cmd.registerConverter(Duration::class.java, DurationConverter())

        val sw = StringWriter()
        cmd.out = PrintWriter(sw)

        val exitCode = cmd.execute("system", "createInfrastructure", "-a")
        println("Done")
    }

    @Test
    fun testServerCreation() {
        val mainConfig = MainConfig()
            mainConfig.poolParallelUpdates = 1
            mainConfig.bungeePoolParallelUpdates = 1
        val serversConfig = ServersConfig()
        serversConfig.bungeePool = BungeePool("testbungee", "dockermc-bungee:testing", "1G", 1.0f, 2, null, 25577)

        val injector = Guice.createInjector(DMCTestGuiceModule(mainConfig = mainConfig, serversConfig = serversConfig,
            dockerPort = dockerPort, postgresPort = postgresPort))

        val commons = injector.getInstance(Commons::class.java).createBungeeServer(false)

    }


    // -----  Utility methods  -----

    fun getDocker(host: String, port: Int): DockerClient {
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


}


/**
 * A custom version of the DMCGuiceModule, which overrides the providers, which access external resources
 */
class DMCTestGuiceModule(
    val mainConfig: MainConfig = MainConfig(),
    val serversConfig: ServersConfig = ServersConfig(),
    val serversConfigOld: ServersConfig = ServersConfig(),
    val dockerPort: Int,
    val postgresPort: Int,
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
        return Docker("tcp", "ubuntu1", dockerPort, mainConfig.get())
    }

    @Provides
    @Singleton
    fun provideSessionBuilder(): SessionBuilder {
        return SessionBuilder(Constants.DB_DMC_USER, "admin", HOSTNAME, postgresPort)
    }

    @Provides
    @Singleton
    fun provideRedisPool(): RedisPool {
        return RedisPool("ubuntu1")
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
    fun provideCommons(mainConfig: Config<MainConfig>, @Named("old")serversConfigOld: Config<ServersConfig>,
                            @Named("current") serversConfig: Config<ServersConfig>, docker: Docker, jedisPool: JedisPool,
                            sessionBuilder: SessionBuilder): Commons {
        return Commons(mainConfig, serversConfig, serversConfigOld, docker, jedisPool, sessionBuilder)
    }

}