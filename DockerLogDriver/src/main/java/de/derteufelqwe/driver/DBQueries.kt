package de.derteufelqwe.driver

import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFile
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder
import org.hibernate.Session
import org.omg.CORBA.Object
import javax.persistence.NoResultException
import kotlin.jvm.Throws

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
    fun getVolumeFolder(session: Session, folderName: String, parent: VolumeFolder): VolumeFolder {
        // language=HQL
        val query = """
            SELECT 
                f
            FROM 
                VolumeFolder AS f
            WHERE 
                f.parent = :parentFolder
                AND f.name = :folderName
        """.trimIndent()

        return session.createQuery(query, VolumeFolder::class.java)
            .setParameter("parentFolder", parent)
            .setParameter("folderName", folderName)
            .setMaxResults(1)
            .singleResult
    }


    /**
     * Tries to find a VolumeFile object with the name #fileName and child of #parent
     */
    @Throws(NoResultException::class)
    fun getVolumeFile(session: Session, fileName: String, parent: VolumeFolder): VolumeFile {
        // language=HQL
        val query = """
            SELECT 
                f
            FROM 
                VolumeFile AS f
            WHERE 
                f.parent = :parentFolder
                AND f.name = :fileName
        """.trimIndent()

        return session.createQuery(query, VolumeFile::class.java)
            .setParameter("parentFolder", parent)
            .setParameter("fileName", fileName)
            .setMaxResults(1)
            .singleResult

//        return DBVolumeFileResult(res[0] as Long, res[1] as ByteArray)
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

}

data class DBVolumeFileResult(val id: Long, val dataHash: ByteArray)