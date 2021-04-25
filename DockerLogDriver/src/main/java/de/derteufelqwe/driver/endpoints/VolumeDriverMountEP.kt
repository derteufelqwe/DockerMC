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
import java.io.Serializable
import java.security.MessageDigest
import kotlin.jvm.Throws

class VolumeDriverMountEP(data: String?) : Endpoint<VolumeDriver.RMount, VolumeDriver.Mount>(data) {

    private val log = LogManager.getLogger(javaClass)
    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();
    private val sha256Digest = MessageDigest.getInstance("SHA-256")


    override fun process(request: VolumeDriver.RMount): VolumeDriver.Mount {
        val file = File(DMCLogDriver.VOLUME_PATH + request.volumeName + "/")
        file.mkdirs()
        var error = ""
        if (request.volumeName in DMCLogDriver.MOUNTED_VOLUMES) {
            return VolumeDriver.Mount(error = "Volume ${request.volumeName} is already mounted!")
        }


        if (error == "") {
            DMCLogDriver.MOUNTED_VOLUMES.add(request.volumeName)
        }

        return VolumeDriver.Mount(file.absolutePath, error)

        val tStart = System.currentTimeMillis()
        try {
            request.volumeName?.let {
                traverseDBFolders(file, it)
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
    private fun traverseDBFolders(volumePath: File, volumeName: String) {
        sessionBuilder.execute() { session ->
            val volume = session.get(Volume::class.java, volumeName) ?: throw DMCDriverException("Failed to mount volume. Volume not found.");

            volume.rootFolder?.let {
                saveFolder(volumePath, it)
            }
        }
    }


    private fun saveFile(path: File, volumeFile: VolumeFile) {
        val targetFile = File(path, volumeFile.name)
        targetFile.createNewFile()
        val fileHash = sha256Digest.digest(targetFile.readBytes())

        // Only update if the file is different from the one in the DB
//        if (!fileHash.contentEquals(volumeFile.dataHash)) {
//            val outputBuffer = ByteArray(Zstd.decompressedSize(volumeFile.data.data).toInt())
//            Zstd.decompress(outputBuffer, volumeFile.data.data)
//
//            targetFile.writeBytes(outputBuffer)
//        }
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