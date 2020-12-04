package de.derteufelqwe.bungeeplugin.redis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import lombok.Data;


/**
 * Base class for every message that gets sent over redis publish-subscribe system.
 */
@Data
public abstract class RedisPubSubData {

    private Gson gson = RedisPubSubData.getGson();

    /**
     * The BungeeCord ID of the SENDER.
     */
    @Expose
    private String bungeeCordId;


    public RedisPubSubData() {
        this.bungeeCordId = BungeePlugin.META_DATA.getTaskName();
    }


    private static Gson getGson() {
        return new GsonBuilder()
                .serializeNulls()
                .disableHtmlEscaping()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
    }

    /**
     * Serializes the event
     */
    public String serialize() {
        return this.gson.toJson(this);
    }

    /**
     * Deserializes a redis event
     * @param data
     * @param type
     * @return
     */
    public static RedisPubSubData deserialize(String data, Class<? extends RedisPubSubData> type) {
        return RedisPubSubData.getGson().fromJson(data, type);
    }

}
