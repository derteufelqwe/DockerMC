package de.derteufelqwe.bungeeplugin.runnables;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.istack.NotNull;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.MojangAPIProfile;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.MojangAPIProfileDeserializer;
import de.derteufelqwe.bungeeplugin.utils.mojangapi.PlayerTextureDeserializer;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Downloads a players skin textures. This should be used by Bungees async scheduler to prevent io blocking
 */
public class PlayerSkinDownloadRunnable implements Runnable {

    private SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();
    private final Gson mojangGson = createMojangGson();
    private UUID uuid;


    public PlayerSkinDownloadRunnable(UUID uuid) {
        this.uuid = uuid;
    }


    @Override
    public void run() {
        try (Session session = this.sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBPlayer dbPlayer = session.get(DBPlayer.class, this.uuid);

                MojangAPIProfile profileData = this.downloadPlayerProfileData(dbPlayer.getUuid());
                dbPlayer.setLastSkinUpdate(new Timestamp(System.currentTimeMillis()));

                if (profileData.getTexture() != null) {
                    MojangAPIProfile.PlayerTexture texture = profileData.getTexture();

                    BufferedImage skin = texture.downloadSkinImage();

                    if (skin != null) {
                        dbPlayer.setSkin(skin);
                        session.update(dbPlayer);
                    }
                }

                tx.commit();

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }

        }

    }


    @NotNull
    private Gson createMojangGson() {
        return new GsonBuilder()
                .registerTypeAdapter(MojangAPIProfile.class, new MojangAPIProfileDeserializer())
                .registerTypeAdapter(MojangAPIProfile.PlayerTexture.class, new PlayerTextureDeserializer())
                .create();
    }

    @NotNull
    private MojangAPIProfile downloadPlayerProfileData(@NotNull UUID playerId) {
        String uid = playerId.toString().replace("-", "");

        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uid);
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            yc.getInputStream()));

            String response = "";
            String s;
            while ((s = in.readLine()) != null)
                response += s;
            in.close();

            // If no valid uuid was supplied return a default response
            if (response.equals("")) {
                return new MojangAPIProfile(playerId);
            }

            return mojangGson.fromJson(response, MojangAPIProfile.class);

        } catch (IOException e) {
            return new MojangAPIProfile(playerId);
        }
    }


}
