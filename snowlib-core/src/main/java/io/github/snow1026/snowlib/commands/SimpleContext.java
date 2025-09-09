package io.github.snow1026.snowlib.commands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A simple, immutable implementation of {@link CommandContext}.
 * <p>
 * This class holds information about:
 * <ul>
 *   <li>The command sender</li>
 *   <li>The command label used</li>
 *   <li>The raw string arguments</li>
 *   <li>A map of parsed argument names to their values</li>
 * </ul>
 * Parsed arguments are stored in a case-insensitive manner.
 */
public final class SimpleContext implements CommandContext {
    private final CommandSender sender;
    private final String label;
    private final String[] rawArgs;
    private final Map<String, Object> parsedArguments;

    /**
     * Creates a new {@link SimpleContext}.
     *
     * @param sender  The sender who executed the command.
     * @param label   The command label used.
     * @param rawArgs The raw command arguments.
     */
    public SimpleContext(CommandSender sender, String label, String[] rawArgs) {
        this.sender = sender;
        this.label = label;
        this.rawArgs = rawArgs;
        this.parsedArguments = new HashMap<>();
    }

    /**
     * Adds a parsed argument to the context.
     * <p>
     * Argument names are stored in lowercase for case-insensitive access.
     *
     * @param name  The name of the argument.
     * @param value The value of the argument.
     */
    public void addArgument(String name, Object value) {
        this.parsedArguments.put(name.toLowerCase(Locale.ROOT), value);
    }

    /**
     * Gets the command sender who executed the command.
     *
     * @return The command sender.
     */
    @Override
    @NotNull
    public CommandSender sender() {
        return sender;
    }

    /**
     * Gets the command label used.
     *
     * @return The command label.
     */
    @Override
    @NotNull
    public String label() {
        return label;
    }

    /**
     * Gets the raw arguments passed to the command.
     *
     * @return The raw arguments as a string array.
     */
    @Override
    @NotNull
    public String[] rawArgs() {
        return rawArgs;
    }

    /**
     * Retrieves the value of a parsed argument by name.
     * <p>
     * If the argument does not exist, an exception will be thrown.
     *
     * @param name The name of the argument.
     * @param <T>  The expected type of the argument.
     * @return The value of the argument.
     * @throws IllegalArgumentException if the argument is not found.
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getArgument(@NotNull String name) {
        Object value = parsedArguments.get(name.toLowerCase(Locale.ROOT));
        if (value == null) {
            throw new IllegalArgumentException("No argument found with name: " + name);
        }
        return (T) value;
    }

    /**
     * Retrieves the value of a parsed argument by name,
     * returning a default value if the argument does not exist.
     *
     * @param name         The name of the argument.
     * @param defaultValue The value to return if the argument is not found.
     * @param <T>          The expected type of the argument.
     * @return The argument value, or the default value if not found.
     */
    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getArgument(@NotNull String name, @Nullable T defaultValue) {
        return (T) parsedArguments.getOrDefault(name.toLowerCase(Locale.ROOT), defaultValue);
    }
}
