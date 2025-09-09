package io.github.snow1026.snowlib.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides the context for a command execution.
 * <p>
 * This interface gives access to the command sender, the original arguments,
 * and type-safe, parsed arguments by name.
 */
public interface CommandContext {

    /**
     * Gets the {@link CommandSender} who executed the command.
     *
     * @return The command sender.
     */
    @NotNull
    CommandSender sender();

    /**
     * A convenience method to get the sender as a {@link Player}.
     *
     * @return The sender as a Player, or null if the sender is not a player.
     */
    @Nullable
    default Player getPlayer() {
        return sender() instanceof Player ? (Player) sender() : null;
    }

    /**
     * Gets the alias or command name that was used to execute the command.
     *
     * @return The command label.
     */
    @NotNull
    String label();

    /**
     * Gets the raw, unparsed string arguments.
     *
     * @return An array of the raw arguments.
     */
    @NotNull
    String[] rawArgs();

    /**
     * Retrieves a parsed argument by its name.
     *
     * @param name The name of the argument defined in the {@link CommandNode}.
     * @param <T>  The expected type of the argument.
     * @return The parsed argument value.
     * @throws ClassCastException       if the argument is not of the expected type.
     * @throws IllegalArgumentException if no argument with the given name exists.
     */
    @NotNull
    <T> T getArgument(@NotNull String name);

    /**
     * Retrieves a parsed argument by its name, returning a default value if not found.
     *
     * @param name         The name of the argument.
     * @param defaultValue The value to return if the argument does not exist.
     * @param <T>          The expected type of the argument.
     * @return The parsed argument value or the default value.
     */
    @Nullable
    <T> T getArgument(@NotNull String name, @Nullable T defaultValue);
}
