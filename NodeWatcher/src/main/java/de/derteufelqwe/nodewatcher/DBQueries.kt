package de.derteufelqwe.nodewatcher

import de.derteufelqwe.commons.hibernate.objects.DBServiceHealth
import de.derteufelqwe.commons.hibernate.objects.Node
import de.derteufelqwe.commons.hibernate.objects.PlayerLogin
import org.hibernate.Session
import java.util.*

object DBQueries {

    /**
     * Returns a list of all active DBServices ids
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun getActiveServicesIDs(session: Session): List<String> {
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
    fun getAllRunningTasks(session: Session, serviceID: String): List<DBServiceHealth> {
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

    /**
     * Returns a list of containers, that are running on a certain swarm node
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun getRunningContainersOnNode(session: Session, nodeID: String): List<String> {
        // language=HQL
        val query = """
            SELECT 
                c.id 
            FROM 
                DBContainer AS c
            WHERE 
                c.stopTime IS NULL 
                AND c.node.id = :nodeid
        """.trimIndent()

        return session.createQuery(query)
            .setParameter("nodeid", nodeID)
            .resultList as List<String>
    }

    /**
     * Returns the IDs of all active nodes from the DB
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun getAllActiveNodeIDs(session: Session): List<String> {
        // language=HQL
        val query = """
            SELECT 
                n.id
            FROM 
                Node AS n 
            WHERE 
                n.leaveTime IS NULL
        """.trimIndent()

        return session.createQuery(query).resultList as List<String>
    }

}