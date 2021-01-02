package de.derteufelqwe.bungeeplugin.api;

import com.sun.istack.NotNull;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.events.BungeeRequestPlayerKickEvent;
import de.derteufelqwe.bungeeplugin.exceptions.DmcAPIException;
import de.derteufelqwe.bungeeplugin.redis.PlayerData;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisRequestPlayerKick;
import de.derteufelqwe.commons.exceptions.NotFoundException;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.IPBan;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.annotation.CheckForNull;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

/**
 * API for general BungeeCord stuff
 */
public class BungeeAPI {

    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();
    private SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();


    public BungeeAPI() {

    }


    private void kickPlayer(@NotNull PlayerData playerData, String reason) {
        ProxyServer.getInstance().getPluginManager().callEvent(new BungeeRequestPlayerKickEvent(playerData.getUsername(), reason, new Callback<BungeeRequestPlayerKickEvent>() {
            @Override
            public void done(BungeeRequestPlayerKickEvent bungeeRequestPlayerKickEvent, Throwable throwable) {
                System.out.println("Kicked player " + playerData.getUuid() + ", " + playerData.getUsername() + ".");
            }
        }));

        redisDataManager.sendMessage(new RedisRequestPlayerKick(playerData.getUsername(), reason));
    }

    public void kickPlayer(UUID playerId, String reason) throws NotFoundException {
        PlayerData playerData = this.redisDataManager.getPlayer(playerId);

        if (playerData == null) {
            throw new NotFoundException("Player %s not online.", playerId);
        }

        this.kickPlayer(playerData, reason);
    }

    public void kickPlayer(String playerName, String reason) throws NotFoundException {
        PlayerData playerData = this.redisDataManager.getPlayer(playerName);

        if (playerData == null) {
            throw new NotFoundException("Player %s not online.", playerName);
        }

        this.kickPlayer(playerData, reason);
    }


    @CheckForNull
    public DBPlayer getPlayerFromDB(Session session, String name) throws DmcAPIException {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<DBPlayer> cq = cb.createQuery(DBPlayer.class);
        Root<DBPlayer> root = cq.from(DBPlayer.class);

        cq.select(root).where(cb.equal(root.get("name"), name));

        Query queryRes = session.createQuery(cq);
        List<DBPlayer> res = (List<DBPlayer>) queryRes.getResultList();

        if (res.size() == 0) {
            return null;

        } else if (res.size() == 1) {
            return res.get(0);

        } else {
            throw new DmcAPIException("Found %s users with the name '%s'.", res.size(), name);
        }

    }




}
