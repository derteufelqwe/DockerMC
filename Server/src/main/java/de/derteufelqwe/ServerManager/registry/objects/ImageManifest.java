package de.derteufelqwe.ServerManager.registry.objects;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

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


    public Date getLastModified() {
        if (history.size() == 0)
            throw new RuntimeException("ImageManifest history has 0 entries.");

        return this.history.get(0).getV1Compatibility().getCreated();
    }

}
