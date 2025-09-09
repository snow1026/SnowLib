package io.snow1026.snowlib.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Simple implementation of {@link CommandContext}.
 *
 * @param sender    Command sender.
 * @param label     Command label.
 * @param args      Raw arguments.
 * @param collected Parsed arguments.
 */
public record SimpleContext(CommandSender sender, String label, String[] args, List<Object> collected) implements CommandContext {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T arg(int index) {
        return (T) collected.get(index);
    }

    @Override
    public int argCount() {
        return collected.size();
    }
}
