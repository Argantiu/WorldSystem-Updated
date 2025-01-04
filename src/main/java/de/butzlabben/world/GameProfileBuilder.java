package de.butzlabben.world;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

/* DEPRECATED
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection; */
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * @author Butzlabben
 * @since 26.02.2018
 */
public class GameProfileBuilder {

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeAdapter(GameProfile.class, new GameProfileSerializer())
            .registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer()).create();
    private static final HashMap<UUID, CachedProfile> cache = new HashMap<>();
    private static final Object sync = new Object();
    private static long cacheTime = -1L;

    private static final HttpClient client = HttpClient.newHttpClient();

    public static GameProfile fetch(UUID uuid) throws IOException {
        return fetch(uuid, false);
    }

    public static GameProfile fetch(UUID uuid, boolean forceNew) throws IOException {
        if ((!forceNew) && (cache.containsKey(uuid)) && (cache.get(uuid).isValid())) {
            return cache.get(uuid).profile;
        }

        String url = String.format("https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false",
                UUIDTypeAdapter.fromUUID(uuid));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(java.time.Duration.ofSeconds(5)) // Set timeout for request
                .build();

        synchronized (sync) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String json = response.body();
                    GameProfile result = gson.fromJson(json, GameProfile.class);
                    cache.put(uuid, new CachedProfile(result));
                    return result;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        if ((!forceNew) && (cache.containsKey(uuid))) {
            return cache.get(uuid).profile;
        }
        throw new IOException("Could not connect to mojang servers for unknown player: " + uuid); // + uuid.toString());
    }

    public static GameProfile getProfile(UUID uuid, String name, String skin) {
        return getProfile(uuid, name, skin, null);
    }

    public static GameProfile getProfile(UUID uuid, String name, String skinUrl, String capeUrl) {
        GameProfile profile = new GameProfile(uuid, name);
        boolean cape = (capeUrl != null) && (!capeUrl.isEmpty());

        List<Object> args = new ArrayList<>();
        args.add(Long.valueOf(System.currentTimeMillis()));
        args.add(UUIDTypeAdapter.fromUUID(uuid));
        args.add(name);
        args.add(skinUrl);
        if (cape) {
            args.add(capeUrl);
        }
        profile.getProperties().put("textures",
                new Property("textures",
                        Base64Coder.encodeString(String.format(
                                cape ? "{\"timestamp\":%d,\"profileId\":\"%s\",\"profileName\":\"%s\",\"isPublic\":true,\"textures\":{\"SKIN\":{\"url\":\"%s\"},\"CAPE\":{\"url\":\"%s\"}}}"
                                        : "{\"timestamp\":%d,\"profileId\":\"%s\",\"profileName\":\"%s\",\"isPublic\":true,\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}",
                                args.toArray(new Object[0])))));
        return profile;
    }

    public static void setCacheTime(long time) {
        cacheTime = time;
    }

    private static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {

        public GameProfile deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject object = (JsonObject) json;
            UUID id = object.has("id") ? (UUID) context.deserialize(object.get("id"), UUID.class) : null;
            String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
            GameProfile profile = new GameProfile(id, name);
            if (object.has("properties")) {
                for (Map.Entry<String, Property> prop : ((PropertyMap) context.deserialize(object.get("properties"),
                        PropertyMap.class)).entries()) {
                    profile.getProperties().put(prop.getKey(), prop.getValue());
                }
            }
            return profile;
        }

        public JsonElement serialize(GameProfile profile, Type type, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            if (profile.getId() != null) {
                result.add("id", context.serialize(profile.getId()));
            }
            if (profile.getName() != null) {
                result.addProperty("name", profile.getName());
            }
            if (!profile.getProperties().isEmpty()) {
                result.add("properties", context.serialize(profile.getProperties()));
            }
            return result;
        }
    }

    private static class CachedProfile {
        private final GameProfile profile;

        public CachedProfile(GameProfile profile) {
            this.profile = profile;
        }

        public boolean isValid() {
            return GameProfileBuilder.cacheTime < 0L;
        }
    }
}
