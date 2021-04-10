package de.derteufelqwe.minecraftplugin

import de.derteufelqwe.commons.hibernate.objects.PlayerLogin
import org.hibernate.Session
import java.util.*
import javax.persistence.NoResultException

object DBQueries {

    /**
     * Checks if a player is a member of a bank
     */
    @JvmStatic
    fun checkPlayerIsBankMember(session: Session, playerID: UUID, bankName: String): Boolean {
        // language=HQL
        val query = """
            SELECT 
                COUNT(pb) 
            FROM 
                PlayerToBank AS pb 
            WHERE 
                pb.player.uuid = :pid 
                AND pb.bank.name = :bn
        """.trimIndent()

        try {
            session.createQuery(query, PlayerLogin::class.java)
                .setParameter("playerid", playerID)
                .setParameter("bn", bankName)
                .singleResult
            return true

        } catch (e: NoResultException) {
            return false
        }
    }


    /**
     * Returns the names of all banks
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun getAllBankNames(session: Session): List<String> {
        // language=HQL
        val query = """
            SELECT 
                b.name 
            FROM 
                Bank as b
        """.trimIndent()


        return session.createQuery(query, PlayerLogin::class.java)
            .resultList as List<String>
    }

}