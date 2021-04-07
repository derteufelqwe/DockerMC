package de.derteufelqwe.commons

import de.derteufelqwe.commons.hibernate.objects.DBService
import org.hibernate.Session

object CommonDBQueries {

    /**
     * Returns a list of all active DBServices
     */
    @JvmStatic
    fun getActiveServices(session: Session) : List<DBService> {
        // language=HQL
        val query = """
            SELECT 
                s 
            FROM 
                DBService s 
            WHERE 
                s.active = true
            ORDER BY 
                s.active DESC , s.name
        """.trimIndent()

        return session.createQuery(query, DBService::class.java).resultList;
    }

}