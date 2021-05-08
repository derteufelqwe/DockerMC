package de.derteufelqwe.plugin.volume

import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

/**
 * Contains a list of Volumes, which are stored locally.
 * This is used to delete volumes, which aren't used anymore
 */
data class LocalVolumes(
    val volumes: MutableList<VolumeInfo> = Collections.synchronizedList(ArrayList())
) : Serializable

data class VolumeInfo(
    val volumeName: String,
    val lastUsedTime: Long
) : Serializable
