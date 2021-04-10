package de.derteufelqwe.ServerManager

import de.derteufelqwe.commons.hibernate.objects.DBContainer
import de.derteufelqwe.commons.hibernate.objects.DBService
import de.derteufelqwe.commons.hibernate.objects.DBServiceHealth
import org.hibernate.Session
import org.intellij.lang.annotations.Language
import java.sql.Timestamp
import javax.persistence.NoResultException

/**
 * This class contains all raw database queries, as kotlin supports multi line strings
 */
object DBQueries {

    /**
     * Returns a distinct list of errors of a service in a given time period
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun getLatestServiceErrors(session: Session, serviceID: String, latestTimestamp: Timestamp): List<String> {
        /**
         * Explanation of the WHEREs:
         *  3. Only show recent errors. Otherwise they wouldn't time out
         *  4. Also include errors about a task not being scheduled (no suitable node ...)
         */
        // language=HQL
        val query = """
            SELECT DISTINCT 
                sh.error 
            FROM 
                DBServiceHealth AS sh 
            WHERE 
                sh.service.id=:sid 
                AND sh.taskState != :tstate1 
                AND sh.createdTimestamp >= :ts
                OR (sh.taskState = :tstate2 AND sh.error != '')
        """.trimIndent()

        return session.createQuery(query)
                .setParameter("sid", serviceID)
                .setParameter("ts", latestTimestamp)
                .setParameter("tstate1", DBServiceHealth.TaskState.RUNNING)
                .setParameter("tstate2", DBServiceHealth.TaskState.PENDING)
                .resultList as List<String>;
    }

    /**
     * Returns the latest service health message for a certain error message
     */
    @JvmStatic
    fun getLatestEntryForServiceHealthError(session: Session, serviceID: String, error: String): DBServiceHealth? {
        // language=HQL
        val query = """
            SELECT 
                sh 
            FROM 
                DBServiceHealth AS sh 
            WHERE 
                sh.service.id=:sid 
                AND sh.taskState not in :tstates 
                AND sh.error = :error 
            ORDER BY 
                sh.createdTimestamp DESC
        """.trimIndent()

        try {
            return session.createQuery(query, DBServiceHealth::class.java)
                    .setParameter("sid", serviceID)
                    .setParameterList("tstates", arrayOf(DBServiceHealth.TaskState.RUNNING, DBServiceHealth.TaskState.STARTING, DBServiceHealth.TaskState.SHUTDOWN))
                    .setParameter("error", error)
                    .setMaxResults(1)
                    .singleResult;

        } catch (e: NoResultException) {
            return null;
        }
    }

    /**
     * Returns a list of all DBServices active and inactive
     */
    @JvmStatic
    fun getAllServices(session: Session) : List<DBService> {
        // language=HQL
        val query = """
            SELECT 
                s 
            FROM 
                DBService s 
            ORDER BY 
                s.active DESC , s.name
        """.trimIndent()

        return session.createQuery(query, DBService::class.java).resultList;
    }

    /**
     * Returns a list of DBContainers
     */
    @JvmStatic
    fun getContainers(session: Session, all : Boolean?, serviceID: String?) : List<DBContainer> {
        var allQuery = ""
        if (all == false) {
            allQuery = "AND c.exitcode IS NULL";
        }

        var serviceQuery = ""
        if (serviceID != null) {
            serviceQuery = "AND c.service.id = '${serviceID}'"
        }

        // language=HQL
        val query = """
            SELECT 
                c 
            FROM 
                DBContainer c 
            WHERE 
                c IS NOT NULL
                ${allQuery}
                ${serviceQuery}
            ORDER BY 
                c.service.id DESC , c.startTime
        """.trimIndent().format()

        return session.createQuery(query, DBContainer::class.java).resultList;
    }

}