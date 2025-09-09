package io.github.snow1026.snowlib.commands.handler;

import io.github.snow1026.snowlib.exceptions.CommandParseException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * The default {@link ExceptionHandler} implementation.
 * <p>
 * It sends the exception's message directly to the command sender.
 */
public class DefaultExceptionHandler implements ExceptionHandler {

    @Override
    public void handle(@NotNull CommandSender sender, @NotNull CommandParseException exception) {
        // In a production environment, you might want to log unexpected errors (those with a cause)
        if (exception.getCause() != null) {
            throw new RuntimeException(exception.getCause());
        }
        sender.sendMessage(exception.getMessage());
    }
}
