package io.github.snow1026.snowlib.commands.argument;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A registry for {@link ArgumentParser} instances.
 * <p>
 * This class provides default parsers for common Bukkit and Java types and allows
 * for the registration of custom parsers.
 */
public final class ArgumentParsers {

    private static final Map<Class<?>, ArgumentParser<?>> parsers = new HashMap<>();

    static {
        // Standard Java Types
        register(String.class, (s, input) -> input);
        register(Integer.class, (s, input) -> {
            try { return Integer.parseInt(input); } catch (NumberFormatException e) { return null; }
        });
        register(Double.class, (s, input) -> {
            try { return Double.parseDouble(input); } catch (NumberFormatException e) { return null; }
        });
        register(Boolean.class, (s, input) -> {
            if (input.equalsIgnoreCase("true")) return true;
            if (input.equalsIgnoreCase("false")) return false;
            return null;
        });

        // Bukkit Types
        register(Player.class, new ArgumentParser<>() {
            @Override
            public Player parse(@NotNull org.bukkit.command.CommandSender sender, @NotNull String input) {
                return Bukkit.getPlayerExact(input);
            }

            @Override
            public @NotNull List<String> suggest(@NotNull org.bukkit.command.CommandSender sender, @NotNull String input) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            }
        });

        register(OfflinePlayer.class, (s, input) -> Bukkit.getOfflinePlayer(input));

        register(World.class, new ArgumentParser<>() {
            @Override
            public World parse(@NotNull org.bukkit.command.CommandSender sender, @NotNull String input) {
                return Bukkit.getWorld(input);
            }

            @Override
            public @NotNull List<String> suggest(@NotNull org.bukkit.command.CommandSender sender, @NotNull String input) {
                return Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
            }
        });
    }

    private ArgumentParsers() {}

    /**
     * Registers a new argument parser for a specific type.
     *
     * @param type   The class of the type to be parsed.
     * @param parser The parser implementation.
     * @param <T>    The type parameter.
     */
    public static <T> void register(@NotNull Class<T> type, @NotNull ArgumentParser<T> parser) {
        parsers.put(type, parser);
    }

    /**
     * Retrieves the registered parser for a given type.
     *
     * @param type The class of the type.
     * @param <T>  The type parameter.
     * @return The registered {@link ArgumentParser}, or {@code null} if none exists.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> ArgumentParser<T> get(@NotNull Class<T> type) {
        return (ArgumentParser<T>) parsers.get(type);
    }
}
