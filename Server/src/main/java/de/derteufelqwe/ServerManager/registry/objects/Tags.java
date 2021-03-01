package de.derteufelqwe.ServerManager.registry.objects;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Tags {

    @SerializedName("name")
    private String name;

    @SerializedName("tags")
    private List<String> tags;

}
