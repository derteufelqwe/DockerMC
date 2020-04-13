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
    @Getter
    private String host;
    @Getter
    private int port;

    public Docker(String host, int port) {
        this.host = host;
        this.port = port;
        this.setupDocker("tcp", host, Integer.toString(port));
    }

    private void setupDocker(String protocol, String host, String port) {
        System.out.println(String.format("Connecting to %s://%s:%s", protocol, host, port));
        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(String.format("%s://%s:%s", protocol, host, port))
                .withApiVersion("1.40")
                .withDockerTlsVerify(false)
//                .withDockerCertPath("/certs")
                .build();

        docker = DockerClientImpl.getInstance(dockerClientConfig)
                .withDockerCmdExecFactory(new NettyDockerCmdExecFactory());
    }

}
