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
import org.jetbrains.annotations.Nullable;

import java.util.*;


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
    private List<String> folders = new ArrayList<>();

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

        for (String folder : folders) {
            mounts.add(createMount(folder, driverName));
        }

        return mounts;
    }

    private Mount createMount(String folderName, @Nullable String driver) {
        // This is required so that you can change the volume driver of a service and don't create a volume name conflict
        String volumeNameAppend = "l";
        if (driver != null) {
            volumeNameAppend = "g";
        }

        String fullName = String.format("%s-{{ .Task.Slot }}-%s", this.name, volumeNameAppend);

        Map<String, String> options = new HashMap<>();
        options.put(Constants.VOLUME_GROUPNAME_KEY, String.format("%s-{{ .Task.Slot }}", this.name));

        Mount mount = new Mount()
                .withSource(String.format("%s-%s", fullName, folderName))    // Full volume name
                .withTarget("/server/" + folderName)
                .withType(MountType.VOLUME)
                .withVolumeOptions(new VolumeOptions()
                        .withDriverConfig(new Driver()
                                .withOptions(options)
                                .withName(driver)
                        )
                );

        return mount;
    }

}

