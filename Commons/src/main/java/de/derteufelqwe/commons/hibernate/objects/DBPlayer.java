package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.annotation.CheckForNull;
import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString(exclude = {"skinData", "onlineStats", "logins", "gottenBans", "executedBans", "executedIpBans", "liftedBans", "liftedIpBans"})
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "players")
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

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    @Column(name = "\"onlineStats\"")
    private List<PlayerOnlineDurations> onlineStats;

    private Timestamp lastOnline;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    private List<PlayerLogin> logins;

    // ----- Ban information -----

    @OneToMany(mappedBy = "bannedPlayer", cascade = CascadeType.ALL)
    @Column(name = "\"gottenBans\"")
    private List<PlayerBan> gottenBans;

    @OneToMany(mappedBy = "bannedBy", cascade = CascadeType.ALL)
    @Column(name = "\"executedBans\"")
    private List<PlayerBan> executedBans;

    @OneToMany(mappedBy = "bannedBy", cascade = CascadeType.ALL)
    private List<IPBan> executedIpBans;

    @OneToMany(mappedBy = "unbannedBy", cascade = CascadeType.ALL)
    private List<PlayerBan> liftedBans;

    @OneToMany(mappedBy = "unbannedBy", cascade = CascadeType.ALL)
    private List<PlayerBan> liftedIpBans;


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
     * Returns the active ban of a player
     *
     * @return
     */
    @CheckForNull
    public PlayerBan getActiveBan() {

        for (PlayerBan ban : this.gottenBans) {
            if (ban.isActive()) {
                return ban;
            }
        }

        return null;
    }

    /**
     * Returns the overall online duration for a player
     *
     * @return
     */
    public long getPlaytime() {
        return this.onlineStats.stream()
                .map(PlayerOnlineDurations::getDuration)
                .reduce(0, Integer::sum)
                .longValue();
    }

}
