package de.derteufelqwe.ServerManager.registry.objects;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Manifest {

    @SerializedName("schemaVersion")
    private int schemaVersion;

    @SerializedName("mediaType")
    private String mediaType;

}
