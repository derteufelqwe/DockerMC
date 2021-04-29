package de.derteufelqwe.plugin.misc

import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import org.hibernate.Session
import javax.persistence.NoResultException

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
     * Returns a list of all volumes
     */
    @JvmStatic
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

