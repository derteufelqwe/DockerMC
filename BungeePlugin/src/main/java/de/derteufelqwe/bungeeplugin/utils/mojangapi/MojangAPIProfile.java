package de.derteufelqwe.bungeeplugin.utils.mojangapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.CheckForNull;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MojangAPIProfile {

    private String id;
    private String name;
    private boolean legacy;

    private PlayerTexture texture;

    /**
     * Used when no response was gotten
     * @param uid
     */
    public MojangAPIProfile(UUID uid) {
        this.id = uid.toString().replace("-", "");
        this.texture = new PlayerTexture(
                null, uid, null, null, null
        );
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class PlayerTexture {

        private Timestamp timestamp;
        private UUID profileId;
        private String profileName;

        private String skinUrl;
        private String capeUrl;


        public boolean isSteve() {
            return (this.profileId.hashCode() & 1) != 0;
        }

        public boolean isAlex() {
            return (this.profileId.hashCode() & 1) == 0;
        }

        @CheckForNull
        public BufferedImage downloadSkinImage() {

            try {
                if (skinUrl == null) {
                    if (this.isSteve()) {
                        return ImageIO.read(getClass().getClassLoader().getResource("skins/defaultSkinSteve.png"));

                    } else {
                        return ImageIO.read(getClass().getClassLoader().getResource("skins/defaultSkinAlex.png"));

                    }
                }

                return ImageIO.read(new URL(this.skinUrl));

            } catch (IOException e) {
                return null;
            }
        }

        @CheckForNull
        public BufferedImage downloadCapeImage() {
            if (capeUrl == null) {
                return null;
            }

            try {
                return ImageIO.read(new URL(this.capeUrl));

            } catch (IOException e) {
                return null;
            }
        }

    }
}
