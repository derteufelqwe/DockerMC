package de.derteufelqwe.nodewatcher

import de.derteufelqwe.commons.hibernate.objects.DBService
import de.derteufelqwe.commons.hibernate.objects.DBServiceHealth
import org.hibernate.Session

object DBQueries {

    /**
     * Returns a list of all active DBServices ids
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun getActiveServicesIDs(session: Session) : List<String> {
        // language=HQL
        val query = """
            SELECT 
                s .id
            FROM 
                DBService s 
            WHERE 
                s.active = true
        """.trimIndent()

        return session.createQuery(query).resultList as List<String>;
    }

    /**
     * Returns a list of all running DBServiceHealths
     */
    @JvmStatic
    fun getAllRunningTasks(session: Session, serviceID : String) : List<DBServiceHealth> {
        // language=HQL
        val query = """
            SELECT 
                sh 
            FROM 
                DBServiceHealth AS sh 
            WHERE 
                sh.taskState not in :tstates
                AND sh.service.id = :sid
        """.trimIndent()

        return session.createQuery(query, DBServiceHealth::class.java)
                .setParameterList("tstates", DBServiceHealth.TaskState.getStoppedStates())
                .setParameter("sid", serviceID)
                .resultList;
    }

}