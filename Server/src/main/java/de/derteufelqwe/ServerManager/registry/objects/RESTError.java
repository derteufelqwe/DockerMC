package de.derteufelqwe.ServerManager.registry.objects;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import javax.validation.constraints.Null;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RESTError {

    @SerializedName("code")
    private String code;

    @SerializedName("message")
    private String message;

    @Nullable
    @SerializedName("detail")
    private String details;

}
