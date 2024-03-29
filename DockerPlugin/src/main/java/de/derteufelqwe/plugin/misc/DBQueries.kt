package de.derteufelqwe.plugin.misc

import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import org.hibernate.Session
import javax.persistence.NoResultException

object DBQueries {

    /**
     * Checks if a container id exists in the DB.
     * This is required instead of a simple 'session.get(...)' because the alternative method might not fail if the id
     * is not in the DB
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


    /**
     * Delete a volume
     */
    @JvmStatic
    fun deleteVolume(session: Session, volume: Volume) {
        // language=HQL
        val query = """
            DELETE FROM
                Volume AS v
            WHERE 
                v = :volume
        """.trimIndent()

        session.createQuery(query)
            .setParameter("volume", volume)
            .executeUpdate()
    }

}

