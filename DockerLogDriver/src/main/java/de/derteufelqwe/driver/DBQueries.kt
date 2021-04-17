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

}