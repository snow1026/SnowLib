package io.github.snow1026.snowlib.command;

import org.bukkit.command.CommandSender;

/**
 * Context object representing a single command execution.
 * Provides access to sender, label, raw args, and parsed arguments.
 */
public interface CommandContext {

    /**
     * Returns the command sender.
     *
     * @return CommandSender.
     */
    CommandSender sender();

    /**
     * Returns the command label.
     *
     * @return Label string.
     */
    String label();

    /**
     * Returns raw arguments.
     *
     * @return Array of Strings.
     */
    String[] args();

    /**
     * Returns parsed argument by index.
     *
     * @param index Index of argument.
     * @param <T>   Type.
     * @return Parsed argument.
     */
    <T> T arg(int index);

    /**
     * Total number of parsed arguments.
     *
     * @return Number of arguments.
     */
    int argCount();
}
