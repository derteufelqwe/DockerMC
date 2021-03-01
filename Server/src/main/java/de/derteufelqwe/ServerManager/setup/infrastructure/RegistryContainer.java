package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.model.*;
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

    /**
     * Remove this Constructor from access. Use the constructor below instead
     */
    public RegistryContainer(String name, String image, String ramLimit, float cpuLimit) {
        super(name, image, ramLimit, cpuLimit, 443);
    }

    public RegistryContainer() {
        super("Registry", Constants.Images.REGISTRY.image(), "1G", 2.0F, 443);
    }

    // -----  Creation methods  -----

    @Override
    protected List<Bind> getBindMounts() {
        List<Bind> mounts = super.getBindMounts();

        mounts.add(
                new Bind("registry_data", new Volume("/var/lib/registry"), false)
        );
        mounts.add(
                new Bind(Constants.REGISTRY_CERT_PATH_2, new Volume("/auth"))
        );
        mounts.add(
                new Bind(Constants.REGISTRY_CERT_PATH_2, new Volume("/certs"))
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
        return 5000;
    }
}
