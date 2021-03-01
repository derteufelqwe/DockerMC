package de.derteufelqwe.commons.hibernate.objects;

import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.objects.economy.Bank;
import de.derteufelqwe.commons.hibernate.objects.economy.PlayerToBank;
import de.derteufelqwe.commons.hibernate.objects.permissions.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import javax.annotation.CheckForNull;
import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString(exclude = {"skinData", "onlineStats", "logins", "gottenBans", "executedBans", "executedIpBans", "liftedBans",
        "liftedIpBans", "additionPermGroups", "ownedBanks", "banks"})
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "players")
@Table(name = "players", indexes = {
        @Index(name = "NAME_INDEX", columnList = "name")
})
public class DBPlayer {

    // ----- General Information -----

    @Id
    private UUID uuid;

    @Type(type = "text")
    private String name;

    /*
     * The timestamp when the user joined the network the first time
     */
    private Timestamp firstJoinDate;

    // ----- Textures -----

    private byte[] skinData;

    private Timestamp lastSkinUpdate;

    // ----- Stats -----


    private Timestamp lastOnline;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PlayerLogin> logins;

    // ----- Permissions -----

    @OneToMany(mappedBy = "player")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PlayerToPermissionGroup> permissionGroups;

    @OneToMany(mappedBy = "player")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Permission> permissions;


    // ----- Ban information -----

    @OneToMany(mappedBy = "bannedPlayer", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PlayerBan> gottenBans;

    @OneToMany(mappedBy = "bannedBy", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PlayerBan> executedBans;

    @OneToMany(mappedBy = "bannedBy", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<IPBan> executedIpBans;

    @OneToMany(mappedBy = "unbannedBy", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PlayerBan> liftedBans;

    @OneToMany(mappedBy = "unbannedBy", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PlayerBan> liftedIpBans;

    // -----  Economy  -----

    private double moneyBalance = 0;

    @OneToMany(mappedBy = "owner")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Bank> ownedBanks = new ArrayList<>();

    @OneToMany(mappedBy = "player")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PlayerToBank> banks = new ArrayList<>();


    public DBPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.firstJoinDate = new Timestamp(System.currentTimeMillis());
    }


    public void setSkin(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);

            this.skinData = out.toByteArray();
        } catch (IOException e) {
            System.err.println("Failed to set image data");
        }

    }

    @CheckForNull
    public BufferedImage getSkin() {
        if (this.skinData == null) {
            return null;
        }

        ByteArrayInputStream in = new ByteArrayInputStream(this.skinData);

        try {
            return ImageIO.read(in);

        } catch (IOException e) {
            System.err.println("Failed to load skin");
            return null;
        }
    }

    /**
     * Returns the active ban of a player or null if no active ban exists
     *
     * @return
     */
    @CheckForNull
    public PlayerBan getActiveBan() {
        if (this.gottenBans == null)
            return null;

        for (PlayerBan ban : this.gottenBans) {
            if (ban.isActive()) {
                return ban;
            }
        }

        return null;
    }

    public boolean isBanned() {
        return this.getActiveBan() != null;
    }

    /**
     * Returns the overall online duration for a player
     *
     * @return
     */
    public long getPlaytime() {
//        return this.onlineStats.stream()
//                .map(PlayerOnlineDurations::getDuration)
//                .reduce(0, Integer::sum)
//                .longValue();

        return -1;
    }


    /**
     * Checks if the player has a certain normal permission
     */
    public boolean hasPermission(String permission) {
        for (Permission perm : this.permissions) {
            if (perm.getPermissionText().equals(permission))
                return true;
        }

        return false;
    }

    /**
     * Checks if the player has a certain service permission
     */
    public boolean hasServicePermission(String permission, DBService service) {
//        for (ServicePermission perm : this.servicePermissions) {
//            if (perm.getPermissionText().equals(permission) && perm.getService().getId().equals(service.getId()))
//                return true;
//        }

        return false;
    }

    /**
     * Checks if the player has a certain timed permission.
     * The timeout is irrelevant
     */
    public boolean hasTimedPermission(String permission) {
//        for (TimedPermission perm : this.timedPermissions) {
//            if (perm.getPermissionText().equals(permission)) {
//                return true;
//            }
//        }

        return false;
    }


    /**
     * Finds a normal permission if available
     */
    @CheckForNull
    public Permission findPermission(String permission) {
        for (Permission perm : this.permissions) {
            if (perm.getPermissionText().equals(permission)) {
                return perm;
            }
        }

        return null;
    }

}
