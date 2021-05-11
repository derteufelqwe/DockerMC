package de.derteufelqwe.plugin.endpoints

import de.derteufelqwe.commons.Utils
import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.plugin.DMCLogDriver
import de.derteufelqwe.plugin.exceptions.VolumeSaveException
import de.derteufelqwe.plugin.messages.VolumeDriver
import de.derteufelqwe.plugin.volume.VolumeInfo
import org.apache.logging.log4j.LogManager
import java.io.Serializable
import java.sql.Timestamp


class VolumeDriverUnmountEP(data: String?) : Endpoint<VolumeDriver.RUnmount, VolumeDriver.Unmount>(data) {

    private val log = LogManager.getLogger(javaClass)
    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();
    private val volumeSaver = DMCLogDriver.getVolumeAutoSaver()


    override fun process(request: VolumeDriver.RUnmount): VolumeDriver.Unmount {
        val volumeName = request.volumeName

        if (volumeName == null) {
            return VolumeDriver.Unmount("Volume name can't be null")

        } else {
            try {
                volumeSaver.saveVolume(volumeName)

            } catch (e: VolumeSaveException) {
                return VolumeDriver.Unmount(e.error)

            } catch (t: Throwable) {
                log.error(t)
                return VolumeDriver.Unmount("Unknown volume save exception ${t.javaClass}: ${t.message}")
            }
        }

        // Save required information about mounted volumes
        DMCLogDriver.MOUNTED_VOLUMES.remove(volumeName)
        DMCLogDriver.getLocalVolumes().volumes.removeIf { it.volumeName == volumeName }
        DMCLogDriver.getLocalVolumes().volumes.add(VolumeInfo(volumeName, System.currentTimeMillis()))
        DMCLogDriver.saveLocalVolumesFile();

        updateVolumeUnmountTimestamp(volumeName)

        return VolumeDriver.Unmount()
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RUnmount::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Unmount::class.java
    }

    private fun updateVolumeUnmountTimestamp(volumeName: String) {
        sessionBuilder.execute { session ->
            val volume = session.get(Volume::class.java, volumeName);
            if (volume == null) {
                log.error("Failed to set volumes unmount time. Volume $volumeName not found.")
                return@execute
            }

            volume.lastUnmounted = Timestamp(System.currentTimeMillis())

            session.update(volume)
        }
    }

}