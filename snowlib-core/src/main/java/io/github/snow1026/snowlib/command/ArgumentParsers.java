package io.github.snow1026.snowlib.command;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for {@link ArgumentParser} instances.
 * <p>
 * Provides default parsers for common types such as String, Integer, Double,
 * Boolean, and {@link Player}.
 */
public class ArgumentParsers {

    private static final Map<Class<?>, ArgumentParser<?>> parsers = new HashMap<>();

    static {
        register(String.class, (s, input) -> input);
        register(Integer.class, (s, input) -> {
            try { return Integer.parseInt(input); } catch (NumberFormatException e) { return null; }
        });
        register(Double.class, (s, input) -> {
            try { return Double.parseDouble(input); } catch (NumberFormatException e) { return null; }
        });
        register(Boolean.class, (s, input) -> Boolean.parseBoolean(input));
        register(Player.class, (s, input) -> Bukkit.getPlayerExact(input));
    }

    /**
     * Registers a new argument parser.
     *
     * @param type   Target type.
     * @param parser Parser implementation.
     * @param <T>    Type parameter.
     */
    public static <T> void register(Class<T> type, ArgumentParser<T> parser) {
        parsers.put(type, parser);
    }

    /**
     * Retrieves a registered parser for a given type.
     *
     * @param type Target type.
     * @param <T>  Type parameter.
     * @return Parser or null if none registered.
     */
    @SuppressWarnings("unchecked")
    public static <T> ArgumentParser<T> get(Class<T> type) {
        return (ArgumentParser<T>) parsers.get(type);
    }
}
