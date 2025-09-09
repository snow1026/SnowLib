package io.github.snow1026.snowlib.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Converts a String argument into a specific type.
 *
 * @param <T> Target type of the argument.
 */
@FunctionalInterface
public interface ArgumentParser<T> {

    /**
     * Parses a String input into the target type.
     *
     * @param sender Command sender.
     * @param input  String argument.
     * @return Parsed value or null if invalid.
     */
    T parse(CommandSender sender, String input);

    /**
     * Returns tab completion suggestions based on partial input.
     *
     * @param sender Command sender.
     * @param input  Partial argument string.
     * @return List of suggestions.
     */
    default List<String> suggest(CommandSender sender, String input) {
        return List.of();
    }
}
