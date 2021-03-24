package de.derteufelqwe.driver.messages;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class Plugin {

    @Data
    @NoArgsConstructor
    public static class RActivate implements Serializable {

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Activate implements Serializable {

        @SerializedName("Implements")
        private List<String> impl = Collections.singletonList("LogDriver");

    }

}
