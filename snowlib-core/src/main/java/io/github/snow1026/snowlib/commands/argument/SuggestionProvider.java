package io.github.snow1026.snowlib.commands.argument;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A functional interface for providing dynamic tab-completion suggestions.
 */
@FunctionalInterface
public interface SuggestionProvider {

    /**
     * Gets a list of suggestions based on the current sender and input.
     *
     * @param sender      The command sender requesting suggestions.
     * @param currentInput The partial argument typed so far.
     * @return A list of suggested completions.
     */
    @NotNull
    List<String> getSuggestions(@NotNull CommandSender sender, @NotNull String currentInput);
}
