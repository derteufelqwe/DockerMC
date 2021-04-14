package de.derteufelqwe.driver.endpoints

import com.github.luben.zstd.Zstd
import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFile
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder
import de.derteufelqwe.driver.DMCLogDriver
import de.derteufelqwe.driver.messages.VolumeDriver
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileInputStream
import java.io.Serializable

class VolumeDriverMountEP(data: String?) : Endpoint<VolumeDriver.RMount, VolumeDriver.Mount>(data) {

    private val log = LogManager.getLogger(javaClass)
    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();


    override fun process(request: VolumeDriver.RMount): VolumeDriver.Mount {
        val file = File(DMCLogDriver.VOLUME_PATH + request.volumeName + "/")
        if (file.exists()) {
            file.deleteRecursively()
        }

        if (request.volumeName == null) {
            return VolumeDriver.Mount(file.absolutePath, "Volume name cant be null")

        } else {
            downloadFiles(file, request.volumeName!!)
        }


        return VolumeDriver.Mount(file.absolutePath, "")
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RMount::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Mount::class.java
    }


    private fun downloadFiles(volumePath: File, volumeName: String) {
        volumePath.mkdirs();

        sessionBuilder.execute() { session ->
            val volume = session.get(Volume::class.java, volumeName);

            volume.files.forEach() {
                saveFile(volumePath, it)
            }

            volume.folders.forEach() {
                saveFolder(volumePath, it)
            }

        }

    }


    private fun saveFile(path: File, file: VolumeFile) {
        val output = ByteArray(file.data.size)
        Zstd.decompress(output, file.data)

        val targetFile = File(file.name)
        targetFile.createNewFile()

        targetFile.writeBytes(output)
    }

    private fun saveFolder(path: File, folder: VolumeFolder) {
        val newPath = File(path, folder.name + "/")

        folder.files.forEach() {
            saveFile(newPath, it)
        }

        folder.folders.forEach() {
            saveFolder(newPath, it)
        }
    }

}