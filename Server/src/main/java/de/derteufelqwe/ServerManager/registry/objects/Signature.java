package de.derteufelqwe.ServerManager.registry.objects;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Signature {

    @SerializedName("header")
    private Map<String, Object> header;

    @SerializedName("signature")
    private String signature;

    @SerializedName("protected")
    private String protec;

}
