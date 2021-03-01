package de.derteufelqwe.ServerManager.registry.objects;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DeleteManifest {

    @SerializedName("schemaVersion")
    private int schemaVersion;

    @SerializedName("mediaType")
    private String mediaType;

    @SerializedName("config")
    private ManifestConfig config;

    @SerializedName("layers")
    private List<DeleteLayer> layers;

}
