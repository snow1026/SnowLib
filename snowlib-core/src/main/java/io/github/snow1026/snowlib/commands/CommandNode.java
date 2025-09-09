package io.github.snow1026.snowlib.commands;

import io.github.snow1026.snowlib.commands.argument.ArgumentParser;
import io.github.snow1026.snowlib.commands.argument.ArgumentParsers;
import io.github.snow1026.snowlib.commands.argument.SuggestionProvider;
import io.github.snow1026.snowlib.exceptions.CommandParseException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents a node in the command tree, allowing for the fluent construction of command syntax.
 * <p>
 * Each node can represent a literal sub-command, a typed argument, or the root of a command.
 * It holds information about its children, execution logic, permissions, and other constraints.
 */
public class CommandNode {

    private final String name;
    private final Class<?> type;
    private final ArgumentParser<?> parser;
    private final Map<String, CommandNode> children = new LinkedHashMap<>();
    private final List<Predicate<CommandSender>> requirements = new ArrayList<>();

    private Consumer<CommandContext> executor;
    private String permission;
    private String permissionMessage = "§cYou do not have permission to use this command.";
    private SuggestionProvider suggestionProvider;

    /**
     * Internal constructor for creating a command node.
     *
     * @param name The name of the node (e.g., argument name or literal value).
     * @param type The data type this node represents.
     */
    protected CommandNode(String name, Class<?> type) {
        this.name = name;
        this.type = type;
        this.parser = ArgumentParsers.get(type);
    }

    /**
     * Appends a required argument to this command node.
     *
     * @param name    The name of the argument, used for retrieval from the {@link CommandContext}.
     * @param type    The class of the argument type (e.g., {@code String.class}, {@code Player.class}).
     * @param builder A consumer to configure the new argument node.
     * @param <T>     The type of the argument.
     * @return This node for chaining.
     */
    public <T> CommandNode argument(@NotNull String name, @NotNull Class<T> type, @NotNull Consumer<CommandNode> builder) {
        CommandNode child = new CommandNode(name, type);
        builder.accept(child);
        children.put(name.toLowerCase(Locale.ROOT), child);
        return this;
    }

    /**
     * Appends a required argument to this command node with no further children.
     *
     * @param name The name of the argument.
     * @param type The class of the argument type.
     * @param <T>  The type of the argument.
     * @return This node for chaining.
     */
    public <T> CommandNode argument(@NotNull String name, @NotNull Class<T> type) {
        return argument(name, type, node -> {});
    }

    /**
     * Appends a literal sub-command to this node. Literals are case-insensitive string constants.
     *
     * @param name    The literal string value.
     * @param builder A consumer to configure the new literal node.
     * @return This node for chaining.
     */
    public CommandNode literal(@NotNull String name, @NotNull Consumer<CommandNode> builder) {
        // Literals are treated as String arguments with a specialized parser and suggester
        CommandNode child = new CommandNode(name, String.class) {
            @Override
            protected @NotNull Object parseValue(CommandSender sender, String input) throws CommandParseException {
                if (input.equalsIgnoreCase(name)) {
                    return name;
                }
                throw new CommandParseException("Expected '" + name + "', but got '" + input + "'");
            }
        };
        child.suggests((sender, current) -> name.toLowerCase(Locale.ROOT).startsWith(current.toLowerCase(Locale.ROOT)) ? List.of(name) : Collections.emptyList());

        builder.accept(child);
        children.put(name.toLowerCase(Locale.ROOT), child);
        return this;
    }

    /**
     * Appends a literal sub-command with no further children.
     *
     * @param name The literal string value.
     * @return This node for chaining.
     */
    public CommandNode literal(@NotNull String name) {
        return literal(name, node -> {});
    }

    /**
     * Defines the action to be performed when this node is successfully reached and executed.
     *
     * @param executor A consumer that accepts the {@link CommandContext} of the execution.
     * @return This node for chaining.
     */
    public CommandNode executes(@NotNull Consumer<CommandContext> executor) {
        this.executor = executor;
        return this;
    }

    /**
     * Sets the permission required to execute this node and any of its children.
     *
     * @param permission The permission string.
     * @return This node for chaining.
     */
    public CommandNode requires(@NotNull String permission) {
        this.permission = permission;
        return this;
    }

    /**
     * Sets a custom message to be displayed when a sender lacks the required permission.
     *
     * @param message The permission denial message.
     * @return This node for chaining.
     */
    public CommandNode permissionMessage(@NotNull String message) {
        this.permissionMessage = message;
        return this;
    }

    /**
     * Adds a generic requirement for executing this node.
     *
     * @param requirement A predicate that must return true for the sender to proceed.
     * @param failMessage The message to send if the requirement is not met.
     * @return This node for chaining.
     */
    public CommandNode requires(@NotNull Predicate<CommandSender> requirement, @NotNull String failMessage) {
        this.requirements.add(sender -> {
            if (!requirement.test(sender)) {
                throw new CommandParseException(failMessage);
            }
            return true;
        });
        return this;
    }

    /**
     * A convenience method that adds a requirement that the command sender must be a {@link Player}.
     *
     * @return This node for chaining.
     */
    public CommandNode playerOnly() {
        return requires(sender -> sender instanceof Player, "§cThis command can only be run by a player.");
    }

    /**
     * Sets a custom provider for tab-completion suggestions for this argument node.
     *
     * @param provider The suggestion provider.
     */
    public void suggests(@NotNull SuggestionProvider provider) {
        this.suggestionProvider = provider;
    }

    // --- Execution and Suggestion Logic ---

    protected void execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        executeRecursive(sender, label, args, 0, new SimpleContext(sender, label, args));
    }

    private void executeRecursive(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args, int index, @NotNull SimpleContext context) {
        checkRequirements(sender);

        // End of arguments, try to execute this node
        if (index >= args.length) {
            if (this.executor != null) {
                this.executor.accept(context);
                return;
            }
            throw new CommandParseException("§cIncomplete command. Please provide more arguments.");
        }

        String currentArg = args[index];

        // Find a child node that can parse the current argument
        for (CommandNode child : children.values()) {
            try {
                Object parsedValue = child.parseValue(sender, currentArg);
                context.addArgument(child.name, parsedValue);
                child.executeRecursive(sender, label, args, index + 1, context);
                return; // Matched and executed
            } catch (CommandParseException e) {
                // This child does not match, try the next one
            }
        }

        // If no child matched, this is an invalid argument
        throw new CommandParseException("§cInvalid argument: '" + currentArg + "'");
    }

    protected List<String> suggest(@NotNull CommandSender sender, @NotNull String[] args) {
        return suggestRecursive(sender, args, 0);
    }

    private List<String> suggestRecursive(@NotNull CommandSender sender, @NotNull String[] args, int index) {
        if (!hasPermission(sender)) return Collections.emptyList();

        if (index >= args.length || args.length == 0) return Collections.emptyList();

        String currentArg = args[index];

        // If this is the last argument fragment, suggest for this level
        if (index == args.length - 1) {
            return children.values().stream()
                    .filter(child -> child.hasPermission(sender))
                    .flatMap(child -> child.getSuggestions(sender, currentArg).stream())
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(currentArg.toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        // If not the last argument, traverse down to the matching child
        String nextArg = args[index];
        for (CommandNode child : children.values()) {
            try {
                child.parseValue(sender, nextArg);
                return child.suggestRecursive(sender, args, index + 1);
            } catch (CommandParseException e) {
                // Not a match
            }
        }

        return Collections.emptyList();
    }

    private void checkRequirements(@NotNull CommandSender sender) {
        if (!hasPermission(sender)) {
            throw new CommandParseException(permissionMessage);
        }
        for (Predicate<CommandSender> requirement : requirements) {
            requirement.test(sender); // Predicate throws exception on failure
        }
    }

    private boolean hasPermission(@NotNull CommandSender sender) {
        return permission == null || sender.hasPermission(permission);
    }

    @NotNull
    protected Object parseValue(CommandSender sender, String input) throws CommandParseException {
        if (parser == null) {
            return input; // No parser means it's a generic string-like type (e.g., the root node)
        }
        Object value = parser.parse(sender, input);
        if (value == null) {
            throw new CommandParseException("Invalid input for type " + type.getSimpleName() + ": '" + input + "'");
        }
        return value;
    }

    @NotNull
    private List<String> getSuggestions(CommandSender sender, String current) {
        if (suggestionProvider != null) {
            return suggestionProvider.getSuggestions(sender, current);
        }
        if (parser != null) {
            return parser.suggest(sender, current);
        }
        return Collections.emptyList();
    }
}
