package de.derteufelqwe.plugin.messages;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class LogDriver {

    @Data
    @NoArgsConstructor
    public static class RCapabilities implements Serializable {

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Capabilities implements Serializable {

        @SerializedName("ReadLogs")
        private boolean readLogs = false;

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RStartLogging implements Serializable {

        @SerializedName("File")
        private String file;

        @SerializedName("Info")
        private Info info;


        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Info implements Serializable {

            @SerializedName("Config")
            private Map<String, String> config;

            @SerializedName("ContainerID")
            private String containerID;

            @SerializedName("ContainerName")
            private String containerName;

            @SerializedName("ContainerEntrypoint")
            private String containerEntrypoint;

            @SerializedName("ContainerArgs")
            private List<String> containerArgs;

            @SerializedName("ContainerImageID")
            private String containerImageID;

            @SerializedName("ContainerImageName")
            private String containerImageName;

            @SerializedName("ContainerCreated")
            private Timestamp containerCreated;

            @SerializedName("ContainerEnv")
            private List<String> containerEnv;

            @SerializedName("ContainerLabels")
            private Map<String, String> containerLabels;

            @SerializedName("LogPath")
            private String logPath;

            @SerializedName("DaemonName")
            private String daemonName;

        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartLogging implements Serializable {

        @SerializedName("Err")
        private String err = "";

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RStopLogging implements Serializable {

        @SerializedName("File")
        private String file;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StopLogging implements Serializable {

        @SerializedName("Err")
        private String err = "";

    }
}
