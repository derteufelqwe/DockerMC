package de.derteufelqwe.ServerManager.setup.templates;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;


/**
 * Template to create a docker container, which has exposed ports
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ExposableContainerTemplate extends ContainerTemplate {

    protected int port;

    public ExposableContainerTemplate(String name, String image, String ramLimit, float cpuLimit, int port) {
        super(name, image, ramLimit, cpuLimit);
        this.port = port;
    }


    // -----  Creation methods  -----

    /**
     * Returns the containers port, that the exposed port gets mapped to.
     * Defaults to the same port number as the exposed port
     */
    protected int getContainerPort() {
        return this.port;
    }

    @Override
    protected List<PortBinding> getPortBindings() {
        List<PortBinding> portBindings = super.getPortBindings();

        // ToDo: Das geht sicherlich auch besser
        portBindings.add(
                new PortBinding(Ports.Binding.bindPort(this.getPort()), ExposedPort.tcp(this.getContainerPort()))
        );

        return portBindings;
    }

}
