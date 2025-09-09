package io.github.snow1026.snowlib.utils;

import com.google.gson.*;
import io.github.snow1026.snowlib.SnowLibrary;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Store is a simple JSON-backed key-value storage system.
 * <p>
 * It allows saving, retrieving, modifying, and removing objects associated
 * with a specific target instance. Supports Bukkit's {@link ConfigurationSerializable}
 * objects, primitive types, arrays, and general JSON-serializable objects.
 * </p>
 *
 * <p>All data is persisted to a single JSON file in the plugin's data folder.</p>
 */
public class Store {

    /** JSON storage file */
    private static final File FILE = new File(SnowLibrary.instance.getDataFolder() + "store");

    /** Gson instance for serialization */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** In-memory representation of stored data */
    private static JsonObject storeData = new JsonObject();

    /** Logger for reporting errors or warnings */
    private static final Logger LOGGER = Logger.getLogger(Store.class.getName());

    // --- 초기 로드 ---

    /**
     * Loads the store from disk into memory.
     * <p>
     * If the storage file does not exist, it will create one automatically.
     * Existing data is parsed and stored in-memory for quick access.
     * </p>
     */
    public static void load() {
        try {
            if (!FILE.exists()) {
                if (FILE.getParentFile() != null && !FILE.getParentFile().exists()) {
                    boolean createdDirs = FILE.getParentFile().mkdirs();
                    if (!createdDirs) {
                        LOGGER.warning("Store: Failed to create directory (" + FILE.getParent() + ")");
                    }
                }
                boolean createdFile = FILE.createNewFile();
                if (!createdFile) {
                    LOGGER.warning("Store: Failed to create file (" + FILE.getPath() + ")");
                }
                save();
            } else {
                String content = Files.readString(FILE.toPath());
                if (!content.isEmpty()) {
                    storeData = JsonParser.parseString(content).getAsJsonObject();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while loading Store", e);
        }
    }

    // --- 저장 ---

    /**
     * Stores a value associated with a target and a specific path.
     * <p>
     * Automatically serializes the target and value to JSON.
     * </p>
     *
     * @param target the object this value is associated with
     * @param value  the object to store
     * @param path   unique path/key for storing the value
     */
    public static void store(Object target, Object value, String path) {
        JsonObject obj = new JsonObject();
        obj.add("target", encode(target));
        obj.add("value", encode(value));
        obj.addProperty("type", value.getClass().getName());

        storeData.add(path, obj);
        save();
    }

    // --- 불러오기 ---

    /**
     * Retrieves a stored value for a given target and path.
     *
     * @param target the associated target object
     * @param path   the key/path to retrieve
     * @return the stored object, or null if not found or target mismatch
     */
    public static Object get(Object target, String path) {
        if (!storeData.has(path)) return null;
        JsonObject obj = storeData.getAsJsonObject(path);

        if (!matchTarget(obj, target)) return null;

        String typeName = obj.get("type").getAsString();
        try {
            Class<?> clazz = Class.forName(typeName);
            return decode(obj.get("value"), clazz);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve value from Store", e);
            return null;
        }
    }

    // --- 수정 ---

    /**
     * Modifies an existing stored value for a target and path.
     * <p>
     * If the path does not exist or target does not match, no operation is performed.
     * </p>
     *
     * @param target   the associated target object
     * @param newValue the new value to store
     * @param path     the key/path to modify
     */
    public static void modify(Object target, Object newValue, String path) {
        if (!storeData.has(path)) return;
        JsonObject obj = storeData.getAsJsonObject(path);

        if (matchTarget(obj, target)) {
            obj.add("value", encode(newValue));
            obj.addProperty("type", newValue.getClass().getName());
            save();
        }
    }

    // --- 삭제 ---

    /**
     * Removes a stored value for a target and path.
     *
     * @param target the associated target object
     * @param path   the key/path to remove
     */
    public static void remove(Object target, String path) {
        if (!storeData.has(path)) return;
        if (matchTarget(storeData.getAsJsonObject(path), target)) {
            storeData.remove(path);
            save();
        }
    }

    // --- 키 목록 ---

    /**
     * Lists all keys stored for a given target.
     *
     * @param target the target object
     * @return a set of all keys associated with this target
     */
    public static Set<String> listKeys(Object target) {
        Set<String> result = new HashSet<>();
        for (String key : storeData.keySet()) {
            JsonObject obj = storeData.getAsJsonObject(key);
            if (matchTarget(obj, target)) result.add(key);
        }
        return result;
    }

    // --- 전체 삭제 ---

    /**
     * Clears all stored entries for a specific target.
     *
     * @param target the target object
     */
    public static void clear(Object target) {
        List<String> toRemove = new ArrayList<>();
        for (String key : storeData.keySet()) {
            if (matchTarget(storeData.getAsJsonObject(key), target)) {
                toRemove.add(key);
            }
        }
        toRemove.forEach(storeData::remove);
        save();
    }

    // --- JSON 직렬화 ---

    /**
     * Serializes an object to JSON.
     * <p>
     * Supports {@link ConfigurationSerializable} objects.
     * </p>
     *
     * @param obj the object to encode
     * @return a JSON object representing the object
     */
    private static JsonObject encode(Object obj) {
        JsonObject result = new JsonObject();
        result.addProperty("class", obj.getClass().getName());

        if (obj instanceof ConfigurationSerializable serializable) {
            result.add("data", GSON.toJsonTree(serializable.serialize()));
        } else {
            result.add("data", GSON.toJsonTree(obj));
        }
        return result;
    }

    // --- JSON 역직렬화 ---

    /**
     * Deserializes a JSON element into a Java object.
     * <p>
     * If the class implements {@link ConfigurationSerializable}, it will attempt
     * to use the {@code deserialize(Map)} static method.
     * </p>
     *
     * @param element the JSON element to decode
     * @param clazz   target class
     * @return deserialized object, or null on failure
     */
    @SuppressWarnings("unchecked")
    private static Object decode(JsonElement element, Class<?> clazz) {
        JsonObject obj = element.getAsJsonObject();
        JsonElement data = obj.get("data");

        if (ConfigurationSerializable.class.isAssignableFrom(clazz)) {
            Map<String, Object> map = GSON.fromJson(data, Map.class);
            try {
                Method method = clazz.getMethod("deserialize", Map.class);
                return method.invoke(null, map);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to deserialize ConfigurationSerializable", e);
            }
        }
        return GSON.fromJson(data, (Type) clazz);
    }

    // --- 타겟 비교 ---

    /**
     * Checks whether a stored JSON entry matches a given target object.
     *
     * @param obj    stored JSON object containing target info
     * @param target the target object to match
     * @return true if target matches, false otherwise
     */
    private static boolean matchTarget(JsonObject obj, Object target) {
        JsonObject stored = obj.get("target").getAsJsonObject();
        String storedClass = stored.get("class").getAsString();
        if (!storedClass.equals(target.getClass().getName())) return false;

        String storedData = stored.get("data").toString();
        String currentData = encode(target).get("data").toString();
        return storedData.equals(currentData);
    }

    // --- 저장 ---

    /**
     * Saves the in-memory store to disk as JSON.
     */
    private static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(storeData, writer);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save Store", e);
        }
    }
}
