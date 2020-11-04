package de.derteufelqwe.logcollector;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;

public class Main {





    @SneakyThrows
    public static void main(String[] args) {
        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(String.format("%s://%s:%s", "tcp", "ubuntu1", 2375))
                .withDockerTlsVerify(false)
                .withApiVersion("1.40")
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig.getDockerHost())
                .sslConfig(dockerClientConfig.getSSLConfig())
                .build();

        DockerClient docker = DockerClientImpl.getInstance(dockerClientConfig, httpClient);

        System.out.println("Connected to docker.");

        docker.eventsCmd()
//                .withLabelFilter("ServerName=LobbyServer")
                .withEventFilter("die")
                .exec(new LogsEventCallback(docker));

        while (true) {
            TimeUnit.SECONDS.sleep(1);
        }

//        docker.close();
    }

}
