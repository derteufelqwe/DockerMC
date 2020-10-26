package de.derteufelqwe.ServerManager.setup.templates;

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

    public ExposableContainerTemplate(String name, String image, String ramLimit, String cpuLimit, int port) {
        super(name, image, ramLimit, cpuLimit);
        this.port = port;
    }

    @Override
    protected List<String> findNullParams() {
        List<String> nullParams = super.findNullParams();

        if (this.port == 0) {
            nullParams.add("port");
        }

        return nullParams;
    }


    // -----  Creation methods  -----

    /**
     * Returns the containers port, that the exposed port gets mapped to.
     * Defaults to the same port number as the exposed port
     */
    protected int getContainerPort() {
        return this.port;
    }

}
