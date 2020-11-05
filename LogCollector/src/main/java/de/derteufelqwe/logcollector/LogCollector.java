package de.derteufelqwe.logcollector;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.SneakyThrows;

public class LogCollector {

    private final String CONTAINER_FILTER = "Owner=DockerMC";
    private final String EVENT_TYPE = "die";

    private DockerClient dockerClient;
    private String host;


    public LogCollector(String host) {
        this.host = host;
        this.dockerClient = this.getDockerClient();
    }


    private DockerClientConfig getDockerClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(host)
                .withDockerTlsVerify(false)
                .withApiVersion("1.40")
                .build();
    }

    private DockerHttpClient getDockerHttpClient() {
        DockerClientConfig clientConfig = this.getDockerClientConfig();

        return new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .build();
    }

    private DockerClient getDockerClient() {
        return DockerClientImpl.getInstance(this.getDockerClientConfig(), this.getDockerHttpClient());
    }

    private SessionBuilder getSessionBuilder() {
//        return new SessionBuilder("admin", "password", "ubuntu1:5432", true);
        return new SessionBuilder("admin", "password", "postgresdb:5432", false);
    }


    public void start() {
        this.dockerClient.pingCmd().exec();

        this.dockerClient.eventsCmd()
                .withLabelFilter(CONTAINER_FILTER)
                .withEventFilter(EVENT_TYPE)
                .exec(new LogsEventCallback(this.dockerClient, this.getSessionBuilder()));
    }

    @SneakyThrows
    public void stop() {
        if (this.dockerClient != null) {
            this.dockerClient.close();
        }
    }

}
