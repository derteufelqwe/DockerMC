package de.derteufelqwe.driver

import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFile
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder
import org.hibernate.Session
import org.omg.CORBA.Object
import java.util.*
import javax.persistence.NoResultException
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.jvm.Throws
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

object DBQueries {

    /**
     * Checks if a container id exists in the DB
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun checkIfContainerExists(session: Session, containerID: String): Boolean {
        // language=HQL
        val query = """
            SELECT 
                c.id
            FROM 
                DBContainer AS c
            WHERE 
                c.id = :cid
        """.trimIndent()

        try {
            session.createQuery(query)
                .setParameter("cid", containerID)
                .setMaxResults(1)
                .singleResult

            return true

        } catch (e: NoResultException) {
            return false
        }
    }


    /**
     * Tries to find a VolumeFolder object with the name #folderName and child of #parent
     */
    @Throws(NoResultException::class)
    fun getVolumeFolders(session: Session, parent: VolumeFolder): Map<String, VolumeFolder> {
        // language=HQL
        val query = """
            SELECT 
                f
            FROM 
                VolumeFolder AS f
            WHERE 
                f.parent = :parentFolder
        """.trimIndent()

        val result = session.createQuery(query, VolumeFolder::class.java)
            .setParameter("parentFolder", parent)
            .resultList

        return result
            .associateBy { it.name }
    }


    @Throws(NoResultException::class)
    fun getVolumeFiles(session: Session, parent: VolumeFolder): Map<String, VolumeFile> {
        // language=HQL
        val query = """
            SELECT 
                f
            FROM 
                VolumeFile AS f
            WHERE 
                f.parent = :parentFolder
        """.trimIndent()

        val result = session.createQuery(query, VolumeFile::class.java)
            .setParameter("parentFolder", parent)
            .resultList

        return result
            .associateBy { it.name }    // Creates the map
    }


    /**
     * Returns a list of all volumes
     */
    fun getAllVolumes(session: Session): List<Volume> {
        // language=HQL
        val query = """
            SELECT 
                v
            FROM 
                Volume AS v
        """.trimIndent()

        return session.createQuery(query, Volume::class.java)
            .resultList
    }


    /**
     * Returns all sub VolumeFolders recursively
     */
    fun getAllVolumeFolders(session: Session, parent: VolumeFolder): Any {
//        // language=SQL
//        val query = """
//            WITH RECURSIVE tmp(id, name, parent_id, volume_id) AS (
//                SELECT
//                    vf.id, vf.name, vf.parent_id, vf.volume_id
//                FROM
//                    volumefolders AS vf
//                WHERE
//                    vf.parent_id = :pid
//                UNION ALL
//                    SELECT
//                           volumefolders.id, volumefolders.name, volumefolders.parent_id, volumefolders.volume_id
//                    FROM
//                         tmp, volumefolders
//                    WHERE
//                        tmp.id = volumefolders.parent_id
//            )
//
//            SELECT * FROM tmp
//        """.trimIndent()
//
//        val result: List<VolumeFolder> = session.createNativeQuery(query, VolumeFolder::class.java)
//            .setParameter("pid", parent.id)
//            .resultList as List<VolumeFolder>


        return ""
    }

}

class VolumeMapObj(val folder: VolumeFolder) {

    val children = VolumeMap(folder)

}

class VolumeMap() : HashMap<String, VolumeMapObj>() {

    constructor(folder: VolumeFolder) : this() {
        insertToMap(this, folder)
    }

    private fun insertToMap(map: VolumeMap, folder: VolumeFolder) {
        for (f in folder.folders) {
            map[f.name] = VolumeMapObj(f)
        }
    }

    fun getFolder(name: String): VolumeFolder? {
        return this[name]?.folder
    }

    fun getChildren(name: String): VolumeMap? {
        return this[name]?.children
    }

}
