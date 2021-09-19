package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;
import de.derteufelqwe.ServerManager.setup.templates.ExposableContainerTemplate;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Represents the registry container where all images get pushed to
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RegistryContainer extends ExposableContainerTemplate {


    private String certBindPath = "";

    public RegistryContainer(String certBindPath) {
        super(Constants.REGISTRY_CONTAINER_NAME, Constants.Images.REGISTRY.image(), "1G", 2.0F, 443);
        this.certBindPath = certBindPath;
    }

    // -----  Creation methods  -----

    @Override
    protected List<Bind> getBindMounts() {
        List<Bind> mounts = super.getBindMounts();

        // ToDo: Make dynamic
        mounts.add(
                new Bind(Constants.REGISTRY_VOLUME_NAME, new Volume("/var/lib/registry"), false)
        );
        mounts.add(
                new Bind(certBindPath, new Volume("/auth"))
        );
        mounts.add(
                new Bind(certBindPath, new Volume("/certs"))
        );

        return mounts;
    }

    @Override
    protected List<String> getEnvironmentVariables() {
        List<String> envs = super.getEnvironmentVariables();

        envs.addAll(Arrays.asList(
                "REGISTRY_AUTH=htpasswd",
                "REGISTRY_AUTH_HTPASSWD_REALM=Registry Realm",
                "REGISTRY_AUTH_HTPASSWD_PATH=/auth/" + Constants.REGISTRY_HTPASSWD_NAME,
                "REGISTRY_HTTP_TLS_CERTIFICATE=/certs/" + Constants.REGISTRY_CERT_NAME,
                "REGISTRY_HTTP_TLS_KEY=/certs/" + Constants.REGISTRY_KEY_NAME,
                "LOGLEVEL=INFO",
                "DEBUG=true",
                "REGISTRY_STORAGE_DELETE_ENABLED=true"
        ));

        return envs;
    }

    @Override
    protected Map<String, String> getContainerLabels() {
        return Utils.quickLabel(Constants.ContainerType.REGISTRY);
    }

    @Override
    protected int getContainerPort() {
        return Constants.REGISTY_CONTAINER_DEFAULT_PORT;
    }

    @Override
    protected List<String> getNetworks() {
        List<String> networks = super.getNetworks();

        networks.add(Constants.NETW_OVERNET_NAME);

        return networks;
    }

    @Override
    protected HostConfig getHostConfig() {
        return super.getHostConfig()
                .withRestartPolicy(RestartPolicy.unlessStoppedRestart());
    }
}
