package de.derteufelqwe.bungeeplugin

import de.derteufelqwe.commons.hibernate.objects.PlayerLogin
import org.hibernate.Session
import java.util.*
import javax.persistence.NoResultException

object DBQueries {

    /**
     * Returns a list of a players logins, which are online
     */
    @JvmStatic
    fun getPlayerLogins(session: Session, playerID: UUID): List<PlayerLogin> {
        // language=HQL
        val query = """
            SELECT 
                pl 
            FROM 
                PlayerLogin AS pl 
            WHERE 
                pl.player.uuid = :playerid 
                AND pl.leaveTime IS NULL
        """.trimIndent()

        return session.createQuery(query, PlayerLogin::class.java)
            .setParameter("playerid", playerID)
            .resultList

    }


}