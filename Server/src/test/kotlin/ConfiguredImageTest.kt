import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import com.google.inject.Guice
import de.derteufelqwe.ServerManager.config.MainConfig
import de.derteufelqwe.ServerManager.config.objects.CertificateCfg
import de.derteufelqwe.ServerManager.utils.Commons
import de.derteufelqwe.junitDocker.DockerRunner
import de.derteufelqwe.junitDocker.util.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Utility class for creating the configured image. This can't be done via dockerfiles because the setup required
 * ServerManager code
 */
@RunWith(DockerRunner::class)
@RequiredClasses([TestUtils::class])
@RemoteJUnitConfig(reuseContainer = false)
class ConfiguredImageTest {


    companion object {

        private val containerStartupDelay = 5000

        @JvmStatic
        @ContainerProvider
        fun containerProvider(): ContainerInfo {
            val config = TestUtils.getConfig()
            val docker = TestUtils.getDocker()

            val container = docker.createContainerCmd(TestConstants.IMAGE_NAME_FULL + ":" + config.imageTagConfigured)
                .withHostConfig(
                    HostConfig()
                        .withPortBindings(
                            PortBinding(Ports.Binding.empty(), ExposedPort.tcp(1099)),
                            PortBinding(Ports.Binding.empty(), ExposedPort.tcp(9876))
                        )
                )
                .withExposedPorts(ExposedPort.tcp(1099), ExposedPort.tcp(9876))
                .withCmd("java", "-jar", "dockermc/DMCServerManager.jar")
                .exec()

            docker.startContainerCmd(container.id).exec()

            // Wait for the container to fully start
            Thread.sleep(containerStartupDelay.toLong())

            println("TestContainer ${container.id} up and running")

            val inspectResp = docker.inspectContainerCmd(container.id).exec()
            val rmiPort = inspectResp.networkSettings.ports.bindings[ExposedPort.tcp(1099)]!![0].hostPortSpec.toInt()
            val logPort = inspectResp.networkSettings.ports.bindings[ExposedPort.tcp(9876)]!![0].hostPortSpec.toInt()

            docker.close()

            return ContainerInfo("ubuntu1", container.id, rmiPort, logPort)
        }

        @JvmStatic
        @ContainerDestroyer
        fun containerDestroyer(info: ContainerInfo) {
            val config = TestUtils.getConfig()
            val docker = TestUtils.getDocker()

            docker.stopContainerCmd(info.containerID).exec()

            var containerStopped = false
            for(i in 0..10) {
                Thread.sleep(2000L)

                if (docker.inspectContainerCmd(info.containerID).exec().state.running == false) {
                    containerStopped = true
                    break
                }
            }
            if (!containerStopped) {
                throw RuntimeException("Failed to stop TestContainer ${info.containerID} in time.")
            }

            println("Stopped TestContainer ${info.containerID}")

            // --- Package the container ---

            val imageID = docker.commitCmd(info.containerID).exec()
            docker.tagImageCmd(imageID, TestConstants.IMAGE_NAME_CONFIGURED, config.imageTagConfigured)

            println("Created configured test image ${imageID.substring(7)} (${TestConstants.IMAGE_NAME_CONFIGURED}:${config.imageTagConfigured}).")
        }

    }


    @Test
    fun createConfiguredImageTest() {
        // Configure the container
        val mainConfig = MainConfig()
        mainConfig.registryCerCfg = CertificateCfg("DE", "BE", "CITY", "TestOrg", "test@test.de")

        val injector = Guice.createInjector(
            DMCTestGuiceModule(
                mainConfig = mainConfig
            )
        )

        TestUtils.injectLogger()

        val commons = injector.getInstance(Commons::class.java)
        commons.createFullInfrastructure()
    }

}


fun main() {

}