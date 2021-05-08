package de.derteufelqwe.plugin.volume

import de.derteufelqwe.commons.misc.RepeatingThread
import de.derteufelqwe.plugin.DMCLogDriver
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Monitors local volumes, which are not used anymore and deletes them if they are older than 24 hours
 */
class LocalVolumeRemover : RepeatingThread(30 * 60000) {

    /**
     * Time in ms after which an unused volume can be removed
     */
    private val VOLUME_REMOVE_TIMEOUT = 24 * TimeUnit.HOURS.toMillis(1)

    private val log = LogManager.getLogger(javaClass)


    override fun repeatedRun() {
        val volumesToRemove = mutableListOf<String>()

        for (volumeInfo in DMCLogDriver.getLocalVolumes().volumes) {
            if (System.currentTimeMillis() >= volumeInfo.lastUsedTime + VOLUME_REMOVE_TIMEOUT) {
                volumesToRemove.add(volumeInfo.volumeName)
            }
        }

        volumesToRemove.forEach(::deleteVolume)
    }


    private fun deleteVolume(volumeName: String) {
        val folder = File(DMCLogDriver.VOLUME_PATH, volumeName)
        if (!folder.exists())
            return

        log.info("Removed obsolete volume '$volumeName' from local filesystem.")
        folder.deleteRecursively()
    }

}
