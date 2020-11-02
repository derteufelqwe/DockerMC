package de.derteufelqwe.ServerManager.setup.templates;

import com.github.dockerjava.api.model.PortConfig;
import com.github.dockerjava.api.model.PortConfigProtocol;
import de.derteufelqwe.ServerManager.exceptions.InvalidConfigException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * Template to create docker services, which has exposed ports
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ExposableServiceTemplate extends ServiceTemplate {

    private int port;

    public ExposableServiceTemplate(String name, String image, String ramLimit, float cpuLimit, int replications, ServiceConstraints constraints, int port) {
        super(name, image, ramLimit, cpuLimit, replications, constraints);
        this.port = port;
    }


    @Override
    public void valid() throws InvalidConfigException {
        super.valid();

        // Port
        if (this.port <= 0) {
            throw new InvalidConfigException("Port can't be 0 or even negative. It should be something above 1000.");
        }

    }


    // -----  Creation methods  -----

    @Override
    protected List<PortConfig> getPortList() {
        List<PortConfig> portList = super.getPortList();

        portList.add(
                new PortConfig().withProtocol(PortConfigProtocol.TCP)
                        .withPublishedPort(this.port)
                        .withTargetPort(this.getContainerPort())
        );

        return portList;
    }

    /**
     * Returns the containers port, that the exposed port gets mapped to.
     * Defaults to the same port number as the exposed port
     */
    protected int getContainerPort() {
        return this.port;
    }

}
