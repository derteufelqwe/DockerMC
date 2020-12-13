package de.derteufelqwe.nodewatcher.misc;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DockerClientFactory {

    private String dockerHost;
    private Map<Long, DockerClient> clientStore = Collections.synchronizedMap(new HashMap<>());


    public DockerClientFactory(String dockerHost) {
        this.dockerHost = dockerHost;
    }


    public synchronized DockerClient getDockerClient() {
        Long threadId = Thread.currentThread().getId();
        DockerClient client = this.clientStore.get(threadId);

        if (client != null) {
            return client;
        }

        client = createDockerClient();
        this.clientStore.put(threadId, client);

        return client;
    }

    public DockerClient forceNewDockerClient() {
        return createDockerClient();
    }

    private DockerClientConfig getDockerClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
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

    private DockerClient createDockerClient() {
        return DockerClientImpl.getInstance(this.getDockerClientConfig(), this.getDockerHttpClient());
    }

}
