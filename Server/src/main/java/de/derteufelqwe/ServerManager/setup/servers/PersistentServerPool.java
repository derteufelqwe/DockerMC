package de.derteufelqwe.ServerManager.setup.servers;

import com.github.dockerjava.api.model.Driver;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
import com.github.dockerjava.api.model.VolumeOptions;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Represents Minecraft server pools, which provide a fixed number of server instances with PERSISTENT data.
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PersistentServerPool extends ServerPool {

    /**
     * Can be 'global' or 'local'
     */
    private String volumeDriver;

    public PersistentServerPool(String name, String image, String ramLimit, float cpuLimit, int replications, ServiceConstraints constraints, int softPlayerLimit) {
        super(name, image, ramLimit, cpuLimit, replications, constraints, softPlayerLimit);
    }


    // -----  Creation methods  -----

    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> containerLabels = super.getContainerLabels();

        containerLabels.putAll(Utils.quickLabel(Constants.ContainerType.MINECRAFT_PERSISTENT));

        return containerLabels;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        Map<String, String> labels = super.getServiceLabels();

        labels.putAll(Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL_PERSISTENT));

        return labels;
    }

    @Override
    protected List<Mount> getMountVolumes() {
        List<Mount> mounts = super.getMountVolumes();

        String driverName = null;
        if (volumeDriver.equals("global")) {
            driverName = Constants.DOCKER_DRIVER_PLUGIN_NAME;
        }

        Mount mount = new Mount()
                .withSource(String.format("%s-{{ .Task.Slot }}", this.name))
                .withTarget("/server")
                .withType(MountType.VOLUME)
                .withVolumeOptions(new VolumeOptions()
                        .withDriverConfig(new Driver()
                                .withName(driverName)
                        )
                );

        mounts.add(mount);

        return mounts;
    }

}

