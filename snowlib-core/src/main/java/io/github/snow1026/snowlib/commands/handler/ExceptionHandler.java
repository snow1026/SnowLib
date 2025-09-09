package io.github.snow1026.snowlib.commands.handler;

import io.github.snow1026.snowlib.exceptions.CommandParseException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * A handler for exceptions that occur during command processing.
 */
@FunctionalInterface
public interface ExceptionHandler {

    /**
     * Handles a {@link CommandParseException} thrown during command execution.
     *
     * @param sender    The sender who executed the command.
     * @param exception The exception that was thrown.
     */
    void handle(@NotNull CommandSender sender, @NotNull CommandParseException exception);
}
