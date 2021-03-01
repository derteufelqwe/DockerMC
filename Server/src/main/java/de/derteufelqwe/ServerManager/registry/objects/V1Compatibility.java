package de.derteufelqwe.ServerManager.registry.objects;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class V1Compatibility {

    @SerializedName("id")
    private String id;

    @SerializedName("container_config")
    private Map<String, Object> containerConfig;

    @SerializedName("created")
    private Date created;

    @Nullable
    @SerializedName("parent")
    private String parent;


    private Map<String, Object> other;

}
