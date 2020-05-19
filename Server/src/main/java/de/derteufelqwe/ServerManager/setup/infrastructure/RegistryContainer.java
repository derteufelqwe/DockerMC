package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.setup.ContainerTemplate;
import de.derteufelqwe.ServerManager.setup.ExposableContainerTemplate;
import de.derteufelqwe.ServerManager.setup.ExposableServiceTemplate;
import de.derteufelqwe.commons.Constants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
    public RegistryContainer(String name, String image, String ramLimit, String cpuLimit) {
        super(name, image, ramLimit, cpuLimit, 443);
    }

    public RegistryContainer() {
        super("Registry", Constants.Images.REGISTRY.image(), "1G", "2", 443);
    }

    // -----  Creation methods  -----

    @Override
    protected List<PortBinding> getPortBindings() {
        List<PortBinding> portBindings = super.getPortBindings();

        // ToDo: Das geht sicherlich auch besser
        portBindings.add(
                new PortBinding(Ports.Binding.bindPort(this.getPort()), ExposedPort.tcp(5000))
        );

        return portBindings;
    }

    @Override
    protected List<Bind> getBindMounts() {
        List<Bind> mounts = super.getBindMounts();

        mounts.add(
                new Bind("registry_data", new Volume("/var/lib/registry"), false)
        );
        mounts.add(
                new Bind(Constants.REGISTRY_CERT_PATH, new Volume("/auth"))
        );
        mounts.add(
                new Bind(Constants.REGISTRY_CERT_PATH, new Volume("/certs"))
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
                "DEBUG=true"
        ));

        return envs;
    }

    @Override
    protected Map<String, String> getContainerLabels() {
        return Utils.quickLabel(Constants.ContainerType.REGISTRY);
    }

}
