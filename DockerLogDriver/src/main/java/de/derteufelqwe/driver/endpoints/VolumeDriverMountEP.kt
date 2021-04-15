package de.derteufelqwe.driver.endpoints

import com.github.luben.zstd.Zstd
import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFile
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder
import de.derteufelqwe.driver.DMCLogDriver
import de.derteufelqwe.driver.exceptions.DMCDriverException
import de.derteufelqwe.driver.messages.VolumeDriver
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileInputStream
import java.io.Serializable
import kotlin.jvm.Throws

class VolumeDriverMountEP(data: String?) : Endpoint<VolumeDriver.RMount, VolumeDriver.Mount>(data) {

    private val log = LogManager.getLogger(javaClass)
    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();


    override fun process(request: VolumeDriver.RMount): VolumeDriver.Mount {
        val file = File(DMCLogDriver.VOLUME_PATH + request.volumeName + "/")
        if (file.exists()) {
            file.deleteRecursively()
        }
        file.mkdirs()

        val tStart = System.currentTimeMillis()
        try {
            request.volumeName?.let {
                downloadFiles(file, it)
                return VolumeDriver.Mount(file.absolutePath, "")
            }

        } catch (e: Exception) {
            log.error("Volume mount failed.", e)
            return VolumeDriver.Mount(file.absolutePath, "Volume mount failed. Error: ${e.message}")
        }

        log.debug(log.trace("Mounting volume ${request.volumeName} took ${System.currentTimeMillis() - tStart}ms."))

        return VolumeDriver.Mount(file.absolutePath, "Volume name cant be null")
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RMount::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Mount::class.java
    }

    @Throws(DMCDriverException::class)
    private fun downloadFiles(volumePath: File, volumeName: String) {
        sessionBuilder.execute() { session ->
            val volume = session.get(Volume::class.java, volumeName)
                ?: throw DMCDriverException("Failed to mount volume. Volume not found.");

            if (volume.rootFolder == null) {
                return@execute
            }

            saveFolder(volumePath, volume.rootFolder)
        }
    }


    private fun saveFile(path: File, file: VolumeFile) {
        val output = ByteArray(Zstd.decompressedSize(file.data).toInt())
        Zstd.decompress(output, file.data)

        val targetFile = File(path, file.name)
        targetFile.createNewFile()

        targetFile.writeBytes(output)
    }

    private fun saveFolder(path: File, folder: VolumeFolder) {
        val newPath = File(path, folder.name)
        newPath.mkdirs()

        folder.files.forEach() {
            saveFile(newPath, it)
        }

        folder.folders.forEach() {
            saveFolder(newPath, it)
        }
    }

}