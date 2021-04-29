package de.derteufelqwe.plugin.messages

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class VolumeDriver {

    class RCapabilities : Serializable

    data class Capabilities(
        @SerializedName("Capabilities") var capabilities: Caps = Caps(),
    ) : Serializable {

        data class Caps(
            @SerializedName("Scope") var scope: String = "global"
        ) : Serializable
    }


    data class RGet(
        @SerializedName("Name") var volumeName: String? = null
    ) : Serializable

    data class Get(
        @SerializedName("Volume") var volume: Volume? = null,
        @SerializedName("Err") var error: String? = null
    ) : Serializable


    class RCreate(
        @SerializedName("Name") var name: String? = null,
        @SerializedName("Opts") var opts: Map<String, String> = HashMap()
    ) : Serializable

    data class Create(
        @SerializedName("Err") var error: String? = null
    ) : Serializable


    data class RRemove(
        @SerializedName("Name") var volumeName: String? = null
    ) : Serializable

    data class Remove(
        @SerializedName("Err") var error: String? = null
    ) : Serializable


    data class RMount(
        @SerializedName("Name") var volumeName: String? = null,
        @SerializedName("ID") var id: String? = null
    ) : Serializable

    data class Mount(
        @SerializedName("Mountpoint") var mountpoint: String? = null,
        @SerializedName("Err") var error: String? = null
    ) : Serializable


    data class RUnmount(
        @SerializedName("Name") var volumeName: String? = null,
        @SerializedName("ID") var id: String? = null
    ) : Serializable

    data class Unmount(
        @SerializedName("Err") var error: String? = null
    ) : Serializable


    data class RPath(
        @SerializedName("Name") var volumeName: String? = null
    ) : Serializable

    data class Path(
        @SerializedName("Mountpoint") var mountpoint: String? = null,
        @SerializedName("Err") var error: String? = null
    ) : Serializable


    class RList : Serializable

    data class List(
        @SerializedName("Volumes") var volumes: kotlin.collections.List<Volume>? = null,
        @SerializedName("Err") var error: String? = null
    ) : Serializable


    // -----  Sub classes  -----


    data class Volume(
        @SerializedName("Name") var name: String? = null,
        @SerializedName("Mountpoint") var mountPoint: String? = null,
        @SerializedName("Status") var status: Map<String, String>? = null,
        @SerializedName("CreatedAt") var created: String? = null
    ) : Serializable
}
