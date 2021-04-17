package de.derteufelqwe.driver.endpoints

import com.github.luben.zstd.Zstd
import de.derteufelqwe.commons.Constants
import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFile
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeObject
import de.derteufelqwe.driver.DBQueries
import de.derteufelqwe.driver.DMCLogDriver
import de.derteufelqwe.driver.Utils
import de.derteufelqwe.driver.messages.VolumeDriver
import org.apache.logging.log4j.LogManager
import org.hibernate.Session
import java.io.File
import java.io.Serializable
import java.security.MessageDigest
import java.sql.Timestamp
import javax.persistence.NoResultException
import kotlin.system.measureTimeMillis


class VolumeDriverUnmountEP(data: String?) : Endpoint<VolumeDriver.RUnmount, VolumeDriver.Unmount>(data) {

    private val log = LogManager.getLogger(javaClass)
    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();
    private val sha256Digest = MessageDigest.getInstance("SHA-256")
    private var hashDuration = 0L;
    private var getOrCreateFileDuration = 0L;
    private var indexFilesDuration = 0L;
    private var indexFoldersDuration = 0L;
    private var indexFilesCounter = 0


    override fun process(request: VolumeDriver.RUnmount): VolumeDriver.Unmount {
        var error = "";
        val volumeRoot = File(DMCLogDriver.VOLUME_PATH + request.volumeName)

        val tStart = System.currentTimeMillis()
        sessionBuilder.execute() { session ->
            try {
                val volume = session.get(Volume::class.java, request.volumeName)

                volume.rootFolder?.let {
                    traverseFolder(session, volumeRoot, it)
                    return@execute
                }

                log.error("Volume ${volume.id} has no root folder!")

            } catch (e: NoResultException) {
                error = "Volume with name ${request.volumeName} not found"

            } catch (e2: Exception) {
                log.error("Processing unmount failed.", e2)
                error = "Generic error ${e2.message}"
            }
        }
        log.debug("Unmounting volume ${request.volumeName} took ${System.currentTimeMillis() - tStart}ms.")
        log.debug("Hashing files took $hashDuration ms.")
        log.debug("Creating new files took $getOrCreateFileDuration ms.")
        log.debug("Getting files from DB took $indexFilesDuration ms.")
        log.debug("Getting folders from DB took $indexFoldersDuration ms.")
        log.debug("Indexed $indexFilesCounter times.")
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

        var availableFiles: Map<String, VolumeFile>
        indexFilesDuration += measureTimeMillis {
            availableFiles = DBQueries.getVolumeFiles(session, parent)
            indexFilesCounter++
        }
        var availableFolders: Map<String, VolumeFolder>
        indexFoldersDuration += measureTimeMillis {
            availableFolders = DBQueries.getVolumeFolders(session, parent)
        }

        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val volumeFolder = availableFolders.getOrElse(file.name) { createNewVolFolder(session, file, parent) }

                session.saveOrUpdate(volumeFolder)
                traverseFolder(session, file, volumeFolder)

            } else {
                addFileToDB(session, file, parent, availableFiles)
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

    private fun createNewVolFolder(
        session: Session,
        folder: File,
        parent: VolumeFolder
    ): VolumeFolder {
        val volumeFolder = VolumeFolder(folder.name)
        volumeFolder.parent = parent

        session.persist(volumeFolder)

        return volumeFolder
    }

    private fun addFileToDB(session: Session, file: File, parent: VolumeFolder, availableFiles: Map<String, VolumeFile>) {
        var volumeFile = VolumeFile()
        getOrCreateFileDuration += measureTimeMillis {
            volumeFile = availableFiles.getOrElse(file.name) { createNewVolFile(session, file, parent) }
        }

        var fileContent = byteArrayOf()
        var dataHash: Long
        hashDuration += measureTimeMillis {
            dataHash = Utils.hashFile(file)
        }

        fileContent = file.readBytes()
        // Only update the file if it changed
        if (volumeFile.dataHash != dataHash) {
            volumeFile.lastModified = Timestamp(System.currentTimeMillis())
            volumeFile.dataHash = dataHash
            volumeFile.data.data = Zstd.compress(fileContent, Constants.ZSTD_COMPRESSION_LEVEL)
        }

        session.update(volumeFile)
    }

    private fun createNewVolFile(
        session: Session,
        file: File,
        parent: VolumeFolder
    ): VolumeFile {
        val volumeFile = VolumeFile(file.name)
        volumeFile.parent = parent
        val volData = VolumeObject()
        volData.file = volumeFile   // Somehow both directions are required
        volumeFile.data = volData

        session.persist(volumeFile)
        session.persist(volData)

        return volumeFile
    }

}