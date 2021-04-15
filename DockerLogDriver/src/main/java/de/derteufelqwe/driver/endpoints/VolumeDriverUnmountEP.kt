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

        val tStart = System.currentTimeMillis()
        sessionBuilder.execute() { session ->
            try {
                val volume = session.get(Volume::class.java, request.volumeName)
                if (volume.rootFolder == null) {
                    log.error("Volume ${volume.id} has no root folder!")
                    return@execute
                }

                traverseFolder(session, volumeRoot, volume.rootFolder)

            } catch (e: NoResultException) {
                error = "Volume with name ${request.volumeName} not found"

            } catch (e2: Exception) {
                log.error("Processing unmount failed.", e2)
                error = "Generic error ${e2.message}"
            }
        }
        log.debug("Unmounting volume ${request.volumeName} took ${System.currentTimeMillis() - tStart}ms.")

        return VolumeDriver.Unmount(error)
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RUnmount::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Unmount::class.java
    }


    private fun traverseFolder(session: Session, folder: File, parent: VolumeFolder) {
        cleanRemovedFiles(session, folder, parent)

        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val volumeFolder = getOrCreateFolder(session, file, parent)

                session.saveOrUpdate(volumeFolder)
                traverseFolder(session, file, volumeFolder)

            } else {
                addFileToDB(session, file, parent)
            }
        }
    }

    /**
     * Removes the files from the DB, which are no longer present on the file system
     */
    private fun cleanRemovedFiles(session: Session, folder: File, parent: VolumeFolder) {
        val localFiles = (folder.listFiles() ?: arrayOf<File>())
            .mapNotNull(File::getName)

        parent.files.forEach { file ->
            if (!localFiles.contains(file.name)) {
                session.delete(file)
            }
        }

    }

    private fun getOrCreateFolder(
        session: Session,
        folder: File,
        parent: VolumeFolder
    ): VolumeFolder {
        try {
            return DBQueries.getVolumeFolder(session, folder.name, parent)

        } catch (e: NoResultException) {
            val volumeFolder = VolumeFolder(folder.name)
            volumeFolder.parent = parent

            return volumeFolder
        }
    }

    private fun addFileToDB(session: Session, file: File, parent: VolumeFolder) {
        val volumeFile = getOrCreateFile(session, file, parent)
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
        parent: VolumeFolder
    ): VolumeFile {
        try {
             return DBQueries.getVolumeFile(session, file.name, parent)

        } catch (e: NoResultException) {
            val volumeFile = VolumeFile(file.name)
            volumeFile.parent = parent

            return volumeFile
        }
    }

}