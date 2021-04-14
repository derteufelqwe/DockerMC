package de.derteufelqwe.driver.endpoints

import com.github.luben.zstd.Zstd
import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFile
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder
import de.derteufelqwe.driver.DBQueries
import de.derteufelqwe.driver.DMCLogDriver
import de.derteufelqwe.driver.messages.VolumeDriver
import lombok.extern.log4j.Log4j2
import org.apache.logging.log4j.LogManager
import org.hibernate.Session
import java.io.File
import java.io.Serializable
import java.security.MessageDigest
import java.sql.Timestamp
import javax.persistence.NoResultException


class VolumeDriverUnmountEP(data: String?) : Endpoint<VolumeDriver.RUnmount, VolumeDriver.Unmount>(data) {

    private val log = LogManager.getLogger(javaClass)
    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();
    private val sha256Digest = MessageDigest.getInstance("SHA-256")


    override fun process(request: VolumeDriver.RUnmount): VolumeDriver.Unmount {
        var error = "";
        val volumeRoot = File(DMCLogDriver.VOLUME_PATH + request.volumeName)

        sessionBuilder.execute() { session ->
            try {
                val volume = session.get(Volume::class.java, request.volumeName)
                traverseFolder(session, volumeRoot, volume = volume)

            } catch (e: NoResultException) {
                error = "Volume with name ${request.volumeName} not found"

            } catch (e2: Exception) {
                log.error("Processing unmount failed.", e2)
                error = "Generic error ${e2.message}"
            }
        }

        return VolumeDriver.Unmount(error)
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RUnmount::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Unmount::class.java
    }


    private fun traverseFolder(session: Session, folder: File, volume: Volume? = null, parent: VolumeFolder? = null) {
        assert(volume != null || parent != null)
        assert(!(volume != null && parent != null))

        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val volumeFolder = getOrCreateFolder(session, file, volume, parent)

                session.saveOrUpdate(volumeFolder)
                traverseFolder(session, file, parent = volumeFolder)

            } else {
                addFileToDB(session, file, volume = volume, parent = parent)
            }
        }
    }

    private fun getOrCreateFolder(
        session: Session,
        folder: File,
        volume: Volume? = null,
        parent: VolumeFolder? = null
    ): VolumeFolder {
        try {
            if (volume != null) {
                return DBQueries.getVolumeFolder(session, folder.name, volume)

            } else if (parent != null) {
                return DBQueries.getVolumeFolder(session, folder.name, parent)

            } else {
                throw RuntimeException("Somehow both volume and parent are null.")
            }

        } catch (e: NoResultException) {
            val volumeFolder = VolumeFolder(folder.name)
            if (volume != null) {
                volumeFolder.volume = volume
            } else {
                volumeFolder.parent = parent
            }

            return volumeFolder
        }
    }

    private fun addFileToDB(session: Session, file: File, volume: Volume? = null, parent: VolumeFolder? = null) {
        assert(volume != null || parent != null)
        assert(!(volume != null && parent != null))

        val volumeFile = getOrCreateFile(session, file, volume, parent)
        volumeFile.lastModified = Timestamp(System.currentTimeMillis())

        val fileContent = file.readBytes()
        val dataHash = sha256Digest.digest(fileContent)

        // Only update the file if it changed
        if (!volumeFile.dataHash.contentEquals(dataHash)) {
            volumeFile.dataHash = dataHash
            volumeFile.data = Zstd.compress(fileContent)
        }

        session.saveOrUpdate(volumeFile)
    }

    private fun getOrCreateFile(
        session: Session,
        file: File,
        volume: Volume? = null,
        parent: VolumeFolder? = null
    ): VolumeFile {
        try {
            if (volume != null) {
                return DBQueries.getVolumeFile(session, file.name, volume)

            } else if (parent != null) {
                return DBQueries.getVolumeFile(session, file.name, parent)

            } else {
                throw RuntimeException("Somehow both volume and parent are null.")
            }

        } catch (e: NoResultException) {
            val volumeFile = VolumeFile(file.name)
            if (volume != null) {
                volumeFile.volume = volume
            } else {
                volumeFile.parent = parent
            }

            return volumeFile
        }
    }

}