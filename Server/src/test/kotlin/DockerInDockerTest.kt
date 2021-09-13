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
import de.derteufelqwe.ServerManager.Docker
import de.derteufelqwe.ServerManager.GuiceFactory
import de.derteufelqwe.ServerManager.cli.CliCommands
import de.derteufelqwe.ServerManager.cli.converters.DurationConverter
import de.derteufelqwe.ServerManager.config.MainConfig
import de.derteufelqwe.ServerManager.config.ServersConfig
import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI
import de.derteufelqwe.ServerManager.setup.servers.BungeePool
import de.derteufelqwe.ServerManager.utils.Commons
import de.derteufelqwe.commons.Constants
import de.derteufelqwe.commons.config.Config
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter
import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.redis.RedisPool
import de.derteufelqwe.junitDocker.DockerRunner
import de.derteufelqwe.junitDocker.util.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.layout.PatternLayout
import org.junit.Test
import org.junit.runner.RunWith
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands
import redis.clients.jedis.JedisPool
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import kotlin.io.path.createTempDirectory


private val IMAGE_NAME = "dockermctest-full:latest"
private val HOSTNAME = "ubuntu1"


@RunWith(DockerRunner::class)
@RequiredClasses([DMCTestGuiceModule::class, PrintAppender::class, TestUtils::class])
@RemoteJUnitConfig(reuseContainer = false)
class DockerInDockerTest {

    private lateinit var localDocker: DockerClient
    private lateinit var docker: DockerClient
    private lateinit var containerID: String
    private var dockerPort = -1
    private var postgresPort = -1


    //    @Before
    fun startUp() {
        this.localDocker = TestUtils.getDocker(HOSTNAME, 2375)

        // Create the docker container to run the tests in
        val container = localDocker.createContainerCmd(IMAGE_NAME)
            .withHostConfig(
                HostConfig()
                    .withRuntime("sysbox-runc")
                    .withPortBindings(
                        PortBinding(Ports.Binding.empty(), ExposedPort.tcp(2375)),
                        PortBinding(Ports.Binding.empty(), ExposedPort.tcp(5432))
                    )
            )
            .exec()

        localDocker.startContainerCmd(container.id).exec()
        containerID = container.id

        // Connect to the test container
        val inspectResp = localDocker.inspectContainerCmd(containerID).exec()
        dockerPort = inspectResp.networkSettings.ports.bindings[ExposedPort.tcp(2375)]!![0].hostPortSpec.toInt()
        postgresPort = inspectResp.networkSettings.ports.bindings[ExposedPort.tcp(5432)]!![0].hostPortSpec.toInt()

        docker = TestUtils.getDocker(HOSTNAME, dockerPort)
        docker.pingCmd().exec()

        println("Connected to docker engine in '$containerID:$dockerPort'.")
    }

    //    @AfterAll
    fun tearDown() {

        docker.close()
        println("Disconnected from docker engine in '$containerID:$dockerPort'.")

        localDocker.killContainerCmd(containerID).exec()
        localDocker.close()
    }


    companion object {
        @JvmStatic
        @ContainerProvider
        fun containerProvider(): ContainerInfo {
            return ContainerInfo("ubuntu1", "", 49173, 49171)
        }

        @JvmStatic
        @ContainerDestroyer
        fun containerDestroyer(info: ContainerInfo) {
            println("")
        }

    }


    @Test
    fun testSimple() {
        println("Testing")
    }

    @Test
    fun testSimple2() {
        println("Testing2")
    }

    //    @Test
    fun testDocker() {
        val injector = Guice.createInjector(DMCTestGuiceModule())
//        // Set up picocli commands
        val commands = CliCommands()
        val factory = PicocliCommands.PicocliCommandsFactory(GuiceFactory(injector))
        val cmd = CommandLine(commands, factory)
        cmd.registerConverter(Duration::class.java, DurationConverter())

        val sw = StringWriter()
        cmd.out = PrintWriter(sw)

//        val exitCode = cmd.execute("system", "listNodes")
        val exitCode = cmd.execute("system", "createInfrastructure", "-n")
        println("Done")
    }

    @Test
    fun testSetupSystem() {
        val mainConfig = MainConfig()
        // Registry config
        mainConfig.registryCerCfg.countryCode = "DE"
        mainConfig.registryCerCfg.state = "BE"
        mainConfig.registryCerCfg.city = "City"
        mainConfig.registryCerCfg.organizationName = "TestOrg"
        mainConfig.registryCerCfg.email = "test@test.de"

        val injector = Guice.createInjector(
            DMCTestGuiceModule(
                mainConfig = mainConfig
            )
        )

        TestUtils.injectLogger()

        val commons = injector.getInstance(Commons::class.java)
        commons.createOvernetNetwork()

        println("Done")
    }

    //    @Test
    fun testServerCreation() {
        val mainConfig = MainConfig()
        mainConfig.poolParallelUpdates = 1
        mainConfig.bungeePoolParallelUpdates = 1
        val serversConfig = ServersConfig()
        serversConfig.bungeePool = BungeePool("testbungee", "dockermc-bungee:testing", "1G", 1.0f, 2, null, 25577)

        val injector = Guice.createInjector(
            DMCTestGuiceModule(
                mainConfig = mainConfig, serversConfig = serversConfig,
            )
        )

        val commons = injector.getInstance(Commons::class.java).createBungeeServer(false)

    }


}