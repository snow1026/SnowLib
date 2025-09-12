package io.github.snow1026.snowlib.commands;

import io.github.snow1026.snowlib.commands.argument.ArgumentParser;
import io.github.snow1026.snowlib.commands.argument.ArgumentParsers;
import io.github.snow1026.snowlib.commands.argument.SuggestionProvider;
import io.github.snow1026.snowlib.exceptions.CommandParseException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class SommandNode {

    private final String name;
    private final Class<?> type;
    private final ArgumentParser<?> parser;
    private final Map<String, SommandNode> children = new LinkedHashMap<>();
    private final List<Predicate<CommandSender>> requirements = new ArrayList<>();
    
    // Only available on the root node to configure the underlying Bukkit command
    private final PluginCommand bukkitCommand;

    private Consumer<SommandContext> executor;
    private String permission;
    private String permissionMessage = "§cYou do not have permission to use this command.";
    private SuggestionProvider suggestionProvider;

    /**
     * Internal constructor for a root command node.
     * @param name The name of the node.
     * @param type The data type this node represents.
     * @param bukkitCommand The associated PluginCommand for configuration.
     */
    protected SommandNode(String name, Class<?> type, @Nullable PluginCommand bukkitCommand) {
        this.name = name;
        this.type = type;
        this.parser = ArgumentParsers.get(type);
        this.bukkitCommand = bukkitCommand;
    }

    /**
     * Internal constructor for child nodes.
     */
    private SommandNode(String name, Class<?> type) {
        this(name, type, null);
    }
    
    // --- New Unified Sub-command Method ---

    /**
     * Appends a literal sub-command to this node. Literals are case-insensitive string constants.
     *
     * @param name    The literal string value.
     * @param builder A consumer to configure the new literal node.
     * @return This node for chaining.
     */
    public SommandNode sub(@NotNull String name, @NotNull Consumer<SommandNode> builder) {
        // Literals are treated as String arguments with a specialized parser and suggester
        SommandNode child = new SommandNode(name, String.class) {
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
    public SommandNode sub(@NotNull String name) {
        return sub(name, node -> {});
    }
    
    /**
     * Appends a required argument to this command node.
     *
     * @param name    The name of the argument, used for retrieval from the {@link SommandContext}.
     * @param type    The class of the argument type (e.g., {@code String.class}, {@code Player.class}).
     * @param builder A consumer to configure the new argument node.
     * @param <T>     The type of the argument.
     * @return This node for chaining.
     */
    public <T> SommandNode sub(@NotNull String name, @NotNull Class<T> type, @NotNull Consumer<SommandNode> builder) {
        SommandNode child = new SommandNode(name, type);
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
    public <T> SommandNode sub(@NotNull String name, @NotNull Class<T> type) {
        return sub(name, type, node -> {});
    }

    // --- Bukkit Command Configuration (for root node) ---

    /**
     * Sets the aliases for this command. Only effective on the root node.
     * @param aliases A list of command aliases.
     * @return This node for chaining.
     */
    public SommandNode alias(@NotNull String... aliases) {
        if (bukkitCommand != null) {
            bukkitCommand.setAliases(Arrays.asList(aliases));
        }
        return this;
    }

    /**
     * Sets the description for this command. Only effective on the root node.
     * @param description The command description.
     * @return This node for chaining.
     */
    public SommandNode description(@NotNull String description) {
        if (bukkitCommand != null) {
            bukkitCommand.setDescription(description);
        }
        return this;
    }

    /**
     * Sets the usage message for this command. Only effective on the root node.
     * @param usage The usage message (e.g., "/<command> [args]").
     * @return This node for chaining.
     */
    public SommandNode usage(@NotNull String usage) {
        if (bukkitCommand != null) {
            bukkitCommand.setUsage(usage);
        }
        return this;
    }

    // --- Standard Node Configuration ---

    /**
     * Defines the action to be performed when this node is successfully reached and executed.
     *
     * @param executor A consumer that accepts the {@link SommandContext} of the execution.
     * @return This node for chaining.
     */
    public SommandNode executes(@NotNull Consumer<SommandContext> executor) {
        this.executor = executor;
        return this;
    }

    /**
     * Sets the permission required to execute this node and any of its children.
     * Also sets the permission on the underlying Bukkit command if this is a root node.
     *
     * @param permission The permission string.
     * @return This node for chaining.
     */
    public SommandNode requires(@NotNull String permission) {
        this.permission = permission;
        if (bukkitCommand != null) {
            bukkitCommand.setPermission(permission);
        }
        return this;
    }

    /**
     * Sets a custom message to be displayed when a sender lacks the required permission.
     * Also sets the permission message on the underlying Bukkit command if this is a root node.
     *
     * @param message The permission denial message.
     * @return This node for chaining.
     */
    public SommandNode permissionMessage(@NotNull String message) {
        this.permissionMessage = message;
        if (bukkitCommand != null) {
            bukkitCommand.setPermissionMessage(message);
        }
        return this;
    }
    
    /**
     * Adds a generic requirement for executing this node.
     *
     * @param requirement A predicate that must return true for the sender to proceed.
     * @param failMessage The message to send if the requirement is not met.
     * @return This node for chaining.
     */
    public SommandNode requires(@NotNull Predicate<CommandSender> requirement, @NotNull String failMessage) {
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
    public SommandNode playerOnly() {
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

        if (index >= args.length) {
            if (this.executor != null) {
                this.executor.accept(context);
                return;
            }
            throw new CommandParseException("§cIncomplete command. See usage: " + (bukkitCommand != null ? bukkitCommand.getUsage() : ""));
        }

        String currentArg = args[index];

        for (SommandNode child : children.values()) {
            try {
                Object parsedValue = child.parseValue(sender, currentArg);
                context.addArgument(child.name, parsedValue);
                child.executeRecursive(sender, label, args, index + 1, context);
                return; 
            } catch (CommandParseException e) {
                // This child does not match, try the next one
            }
        }

        throw new CommandParseException("§cInvalid argument: '" + currentArg + "'");
    }

    protected List<String> suggest(@NotNull CommandSender sender, @NotNull String[] args) {
        return suggestRecursive(sender, args, 0);
    }

    private List<String> suggestRecursive(@NotNull CommandSender sender, @NotNull String[] args, int index) {
        if (!hasPermission(sender)) return Collections.emptyList();

        if (index >= args.length || args.length == 0) return Collections.emptyList();

        String currentArg = args[index];

        if (index == args.length - 1) {
            return children.values().stream()
                    .filter(child -> child.hasPermission(sender))
                    .flatMap(child -> child.getSuggestions(sender, currentArg).stream())
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(currentArg.toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        String nextArg = args[index];
        for (SommandNode child : children.values()) {
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
            // Should only happen for types with no registered parser
            throw new CommandParseException("No parser found for type " + type.getSimpleName());
        }
        Object value = parser.parse(sender, input);
        if (value == null) {
            throw new CommandParseException("Invalid input for " + name + " (expected " + type.getSimpleName() + "): '" + input + "'");
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
