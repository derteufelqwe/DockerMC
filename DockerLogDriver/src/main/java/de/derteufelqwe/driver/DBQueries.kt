package de.derteufelqwe.driver

import de.derteufelqwe.commons.hibernate.objects.DBService
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

}