package de.derteufelqwe.ServerManager.registry.objects;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DeleteLayer {

    @SerializedName("mediaType")
    private String mediaType;

    @SerializedName("size")
    private long size;

    @SerializedName("digest")
    private String digest;

}
