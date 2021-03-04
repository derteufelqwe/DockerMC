package de.derteufelqwe.ServerManager.registry.objects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import de.derteufelqwe.commons.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.CheckForNull;
import java.util.Date;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ImageManifest {

    @SerializedName("schemaVersion")
    private int schemaVersion;

    @SerializedName("name")
    private String name;

    @SerializedName("tag")
    private String tag;

    @SerializedName("architecture")
    private String architecture;

    @SerializedName("fsLayers")
    private List<Layer> layers;

    @SerializedName("history")
    private List<History> history;

    @SerializedName("signatures")
    private List<Signature> signatures;

    @Expose(deserialize = false)
    private String contentDigest;

    public Date getLastModified() {
        if (history.size() == 0)
            throw new RuntimeException("ImageManifest history has 0 entries.");

        return this.history.get(0).getV1Compatibility().getCreated();
    }

    /**
     * Tries to parse the DockerMC image type from the images labels
     * @return The label or null
     */
    @CheckForNull
    @SuppressWarnings("unchecked")
    public String getDMCImageType() {
        for (History history : history) {
            V1Compatibility compatibility = history.getV1Compatibility();

            Map<String, Object> otherData = compatibility.getOther();
            if (otherData == null)
                return null;

            if (!otherData.containsKey("config"))
                return null;
            Map<String, Object> config = (Map<String, Object>) otherData.get("config");


            if (!config.containsKey("Labels"))
                return null;
            Map<String, Object> labels = (Map<String, Object>) config.get("Labels");

            Object label = labels.get(Constants.DOCKER_IMAGE_TYPE_TAG);
            if (label != null)
                return (String) label;
        }

        return null;
    }

}
