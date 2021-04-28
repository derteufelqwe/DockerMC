package de.derteufelqwe.driver.misc

import java.io.Serializable
import java.util.*
import java.util.concurrent.locks.ReentrantLock
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
