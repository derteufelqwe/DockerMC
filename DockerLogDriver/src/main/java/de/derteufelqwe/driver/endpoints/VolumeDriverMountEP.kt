package de.derteufelqwe.driver.endpoints

import com.github.luben.zstd.Zstd
import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFile
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder
import de.derteufelqwe.driver.DMCLogDriver
import de.derteufelqwe.driver.misc.Utils
import de.derteufelqwe.driver.exceptions.VolumeLoadException
import de.derteufelqwe.driver.messages.VolumeDriver
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.Serializable
import kotlin.system.measureTimeMillis

class VolumeDriverMountEP(data: String?) : Endpoint<VolumeDriver.RMount, VolumeDriver.Mount>(data) {

    private val log = LogManager.getLogger(javaClass)
    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();


    override fun process(request: VolumeDriver.RMount): VolumeDriver.Mount {
        val volumeName = request.volumeName ?: return VolumeDriver.Mount(error = "Volume name can't be null.")
        val file = File("${DMCLogDriver.VOLUME_PATH}/$volumeName/")
        file.mkdirs()

        if (volumeName in DMCLogDriver.MOUNTED_VOLUMES) {
            return VolumeDriver.Mount(error = "Volume $volumeName is already mounted!")
        }

        DMCLogDriver.MOUNTED_VOLUMES.add(volumeName)
        val metric = Metric()

        val tStart = System.currentTimeMillis()
        try {
            sessionBuilder.execute { session ->
                val volume = session.get(Volume::class.java, volumeName) ?: throw VolumeLoadException(volumeName, "Volume not found in DB.")
                val rootFolder = volume.rootFolder ?: throw VolumeLoadException(volumeName, "Volume has no root folder.")

                saveFolder(file, rootFolder, metric)
            }

        } catch (e1: VolumeLoadException) {
            DMCLogDriver.MOUNTED_VOLUMES.remove(volumeName)
            return VolumeDriver.Mount(error = e1.error)

        } catch (e: Exception) {
            log.error("Volume mount failed.", e)
            DMCLogDriver.MOUNTED_VOLUMES.remove(volumeName)

            return VolumeDriver.Mount(error = "Volume mount failed. Error: ${e.message}")
        }

        log.trace("Hashing files took ${metric.hashDuration} ms.")
        log.trace("Saving files took ${metric.saveFilesDuration} ms.")
        log.info("Mounting volume ${request.volumeName} took ${System.currentTimeMillis() - tStart}ms.")

        return VolumeDriver.Mount(file.absolutePath, "")
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RMount::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Mount::class.java
    }


    private fun saveFile(path: File, volumeFile: VolumeFile, metric: Metric) {
        val targetFile = File(path, volumeFile.name)
        targetFile.createNewFile()

        val fileHash: Long
        metric.hashDuration += measureTimeMillis {
            fileHash = Utils.hashFile(targetFile)
        }

        // Only update if the file is different from the one in the DB
        if (volumeFile.dataHash != fileHash) {
            metric.saveFilesDuration += measureTimeMillis {
                val outputBuffer = ByteArray(Zstd.decompressedSize(volumeFile.data.data).toInt())
                Zstd.decompress(outputBuffer, volumeFile.data.data)

                targetFile.writeBytes(outputBuffer)
            }
        }
    }

    private fun saveFolder(path: File, folder: VolumeFolder, metric: Metric) {
        val newPath = File(path, folder.name)
        newPath.mkdirs()

        folder.files.forEach() {
            saveFile(newPath, it, metric)
        }

        folder.folders.forEach() {
            saveFolder(newPath, it, metric)
        }
    }

}

private data class Metric(
    var hashDuration: Long = 0L,
    var saveFilesDuration: Long = 0L,
)
