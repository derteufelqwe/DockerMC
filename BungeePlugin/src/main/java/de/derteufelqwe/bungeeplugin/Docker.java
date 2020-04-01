package de.derteufelqwe.bungeeplugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import de.derteufelqwe.commons.Constants;
import lombok.Getter;

public class Docker {

    @Getter
    private DockerClient docker;

    public Docker() {

//        setupDocker("tcp", "localhost", "2375");
//        setupDocker("tcp", "host.docker.internal", "2375");
        setupDocker("tcp", Constants.APIPROXY_CONTAINER_NAME, "443");
    }

    private void setupDocker(String protocol, String host, String port) {
        System.out.println(String.format("Connecting to %s://%s:%s", protocol, host, port));
        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(String.format("%s://%s:%s", protocol, host, port))
                .withApiVersion("1.40")
                .withDockerTlsVerify(true)
                .withDockerCertPath("/certs")
                .build();

        docker = DockerClientImpl.getInstance(dockerClientConfig)
                .withDockerCmdExecFactory(new NettyDockerCmdExecFactory());
    }

}
