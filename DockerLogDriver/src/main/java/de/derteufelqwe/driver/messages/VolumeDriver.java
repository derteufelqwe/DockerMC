package de.derteufelqwe.driver.messages;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

public class VolumeDriver {

    @Data
    @AllArgsConstructor
    public static class RCapabilities implements Serializable {

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Capabilities implements Serializable {

        @SerializedName("Capabilities")
        private Caps capabilities = new Caps();


        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        private static class Caps implements Serializable {

            @SerializedName("Scope")
            private String scope = "local";

        }

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RGet implements Serializable {

        @SerializedName("Name")
        private String volumeName;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Get implements Serializable {

        @SerializedName("Volume")
        private Volume volume;

        @SerializedName("Err")
        private String error;

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RCreate implements Serializable {

        @SerializedName("Name")
        private String name;

        @SerializedName("Opts")
        private Map<String, String> opts;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create implements Serializable {

        @SerializedName("Err")
        private String error;

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RRemove implements Serializable {

        @SerializedName("Name")
        private String volumeName;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Remove implements Serializable {

        @SerializedName("Err")
        private String error;

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RMount implements Serializable {

        @SerializedName("Name")
        private String volumeName;

        @SerializedName("ID")
        private String id;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mount implements Serializable {

        @SerializedName("Mountpoint")
        private String mountpoint;

        @SerializedName("Err")
        private String error;

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RUnmount implements Serializable {

        @SerializedName("Name")
        private String volumeName;

        @SerializedName("ID")
        private String id;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Unmount implements Serializable {

        @SerializedName("Err")
        private String error;

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RPath implements Serializable {

        @SerializedName("Name")
        private String volumeName;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Path implements Serializable {

        @SerializedName("Mountpoint")
        private String mountpoint;

        @SerializedName("Err")
        private String error;

    }


    @Data
    @AllArgsConstructor
    public static class RList implements Serializable {

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class List implements Serializable {

        @SerializedName("Volumes")
        private java.util.List<Volume> volumes;

        @SerializedName("Err")
        private String error;

    }


    // -----  Data classes  -----

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Volume implements Serializable {

        @SerializedName("Name")
        private String name;

        @SerializedName("Mountpoint")
        private String mountPoint;

        @SerializedName("Status")
        private Map<String, String> status;

    }

}
