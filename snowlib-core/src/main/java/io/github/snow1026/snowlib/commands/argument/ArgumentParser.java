package io.github.snow1026.snowlib.commands.argument;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * A functional interface for parsing a string argument into a specific type.
 *
 * @param <T> The target type to parse into.
 */
@FunctionalInterface
public interface ArgumentParser<T> {

    /**
     * Parses a string input into the target type {@code T}.
     *
     * @param sender The sender executing the command.
     * @param input  The raw string argument.
     * @return The parsed value, or {@code null} if parsing fails.
     */
    @Nullable
    T parse(@NotNull CommandSender sender, @NotNull String input);

    /**
     * Provides tab-completion suggestions for a partial argument.
     *
     * @param sender The sender requesting suggestions.
     * @param input  The partial string argument that has been typed so far.
     * @return A list of potential completions.
     */
    @NotNull
    default List<String> suggest(@NotNull CommandSender sender, @NotNull String input) {
        return Collections.emptyList();
    }
}
