package de.derteufelqwe.driver.endpoints

import de.derteufelqwe.driver.DMCLogDriver
import de.derteufelqwe.driver.exceptions.VolumeSaveException
import de.derteufelqwe.driver.messages.VolumeDriver
import de.derteufelqwe.driver.misc.VolumeInfo
import org.apache.logging.log4j.LogManager
import java.io.Serializable


class VolumeDriverUnmountEP(data: String?) : Endpoint<VolumeDriver.RUnmount, VolumeDriver.Unmount>(data) {

    private val log = LogManager.getLogger(javaClass)
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

        return VolumeDriver.Unmount()
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RUnmount::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Unmount::class.java
    }

}