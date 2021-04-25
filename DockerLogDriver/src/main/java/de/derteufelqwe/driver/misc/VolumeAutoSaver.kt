package de.derteufelqwe.driver.misc

import com.github.luben.zstd.Zstd
import de.derteufelqwe.commons.Constants
import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFile
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeObject
import de.derteufelqwe.commons.misc.RepeatingThread
import de.derteufelqwe.driver.DMCLogDriver
import de.derteufelqwe.driver.Utils
import de.derteufelqwe.driver.exceptions.VolumeSaveException
import org.apache.logging.log4j.LogManager
import org.hibernate.Session
import java.io.File
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import javax.persistence.NoResultException
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashSet
import kotlin.jvm.Throws
import kotlin.system.measureTimeMillis

/**
 * Periodically syncs the volume with the DB
 */
class VolumeAutoSaver : RepeatingThread(10000) {

    private val mountedVolumes = DMCLogDriver.MOUNTED_VOLUMES
    private val log = LogManager.getLogger(javaClass)
    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();

    /**
     * A set of VolumeFile IDs, which can be deleted
     */
    private val filesToRemove = LinkedBlockingQueue<Long>()
    /**
     * A set of volumes, which are getting saved currently. This way volumes don't get saved in parallel
     */
    private val savingInProgressVolumes = Collections.synchronizedSet(HashSet<String>())


    override fun repeatedRun() {
        for (volume in HashSet(mountedVolumes)) {
            saveVolume(volume)
        }
    }


    /**
     * Saves a volume to the DB
     */
    @Throws(VolumeSaveException::class)
    fun saveVolume(volumeName: String) {
        this.awaitVolumeSave(volumeName)    // Wait until parallel saves are complete
        val volumeRoot = File(DMCLogDriver.VOLUME_PATH + volumeName)
        val metric = Metric()

        val tStart = System.currentTimeMillis()
        sessionBuilder.execute { session ->
            try {
                savingInProgressVolumes.add(volumeName)
                val volume = session.get(Volume::class.java, volumeName)

                volume.rootFolder?.let {
                    traverseFolder(session, volumeRoot, it, metric)
                    return@execute
                }

                throw VolumeSaveException(volumeName, "Volume has no root folder!")

            } catch (e: NoResultException) {
                throw VolumeSaveException(volumeName, "Volume not found!")

            } catch (e2: Exception) {
                throw VolumeSaveException(volumeName, e2)

            } finally {
                savingInProgressVolumes.remove(volumeName)
            }
        }

        sessionBuilder.execute { session ->
            removeFilesFromDB(session)
        }

        log.debug("Hashing files took ${metric.hashDuration} ms.")
        log.debug("Creating new files took ${metric.getOrCreateFileDuration} ms.")
        log.debug("Getting files from DB took ${metric.indexFilesDuration} ms.")
        log.debug("Getting folders from DB took ${metric.indexFoldersDuration} ms.")

        log.info("Saving volume $volumeName took ${System.currentTimeMillis() - tStart}ms.")
    }

    /**
     * Waits until parallel saves are completed
     */
    private fun awaitVolumeSave(volumeName: String) {
        while (this.savingInProgressVolumes.contains(volumeName)) {
            TimeUnit.MILLISECONDS.sleep(250)
        }
    }

    /**
     * Traverses all files and folders and adds them to the DB
     */
    private fun traverseFolder(session: Session, folder: File, parent: VolumeFolder, metric: Metric) {
        findFilesToRemove(folder, parent)

        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val volumeFolder: VolumeFolder
                metric.indexFoldersDuration += measureTimeMillis {
                    volumeFolder = parent.folders.associateBy { it.name }
                        .getOrElse(file.name) { createNewVolFolder(session, file, parent) }
                }

                traverseFolder(session, file, volumeFolder, metric)

            } else {
                val availableFiles: Map<String, VolumeFile>
                metric.indexFilesDuration += measureTimeMillis {
                    availableFiles = parent.files.associateBy { it.name }
                }

                addFileToDB(session, file, parent, availableFiles, metric)
            }
        }
    }

    /**
     * Adds a file to the DB. If it's identical to the file in the DB nothing happens
     */
    private fun addFileToDB(
        session: Session,
        file: File,
        parent: VolumeFolder,
        availableFiles: Map<String, VolumeFile>,
        metric: Metric
    ) {
        var volumeFile = VolumeFile()
        metric.getOrCreateFileDuration += measureTimeMillis {
            volumeFile = availableFiles.getOrElse(file.name) { createNewVolFile(session, file, parent) }
        }

        var dataHash: Long
        metric.hashDuration += measureTimeMillis {
            dataHash = Utils.hashFile(file)
        }

        val fileContent: ByteArray = file.readBytes()
        // Only update the file if it changed
        if (volumeFile.dataHash != dataHash) {
            volumeFile.lastModified = Timestamp(System.currentTimeMillis())
            volumeFile.dataHash = dataHash
            volumeFile.data.data = Zstd.compress(fileContent, Constants.ZSTD_COMPRESSION_LEVEL)
        }

        session.update(volumeFile)
    }

    /**
     * Creates a new VolumeFolder in the DB
     */
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

    /**
     * Creates a new VolumeFile in the DB
     */
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

    /**
     * Finds all files in the DB, which are no longer present on the file system
     */
    private fun findFilesToRemove(folder: File, parent: VolumeFolder) {
        val localFiles = (folder.listFiles() ?: arrayOf<File>())
            .mapNotNull(File::getName)

        parent.files.forEach { file ->
            if (!localFiles.contains(file.name)) {
                filesToRemove.add(file.id)
            }
        }

    }

    /**
     * Removes the files that are no longer required
     */
    private fun removeFilesFromDB(session: Session) {
        var element = filesToRemove.poll()

        while (element != null) {
            val dbFile = session.get(VolumeFile::class.java, element)
            session.delete(dbFile)

            element = filesToRemove.poll()
        }
    }

}


private data class Metric(
    var hashDuration: Long = 0L,
    var getOrCreateFileDuration: Long = 0L,
    var indexFilesDuration: Long = 0L,
    var indexFoldersDuration: Long = 0L,
)
