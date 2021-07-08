import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import de.derteufelqwe.ServerManager.callbacks.ImagePullCallback
import org.junit.jupiter.api.Test


class DockerInDockerTest {


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


    @Test
    fun testDocker() {
        val docker = getDocker("ubuntu1", 23456)
        docker.pingCmd().exec()

        try {
            docker.inspectImageCmd("hello-world:latest").exec()
            println("Image already present")

        } catch (e: NotFoundException) {
            val pull = docker.pullImageCmd("hello-world:latest").exec(ImagePullCallback())
            pull.awaitSuccess()

        }

        val container = docker.createContainerCmd("hello-world:latest").exec()
        docker.startContainerCmd(container.id).exec()

        val containers = docker.listContainersCmd().withShowAll(true).exec()
        println(containers)

        return
    }

}