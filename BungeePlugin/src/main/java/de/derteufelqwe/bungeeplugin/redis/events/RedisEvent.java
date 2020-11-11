package de.derteufelqwe.bungeeplugin.redis.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import lombok.Data;


@Data
public abstract class RedisEvent {

    private Gson gson = RedisEvent.getGson();
    @Expose
    private String bungeeCordId;


    public RedisEvent() {
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
    public static RedisEvent deserialize(String data, Class<? extends RedisEvent> type) {
        return RedisEvent.getGson().fromJson(data, type);
    }

}
