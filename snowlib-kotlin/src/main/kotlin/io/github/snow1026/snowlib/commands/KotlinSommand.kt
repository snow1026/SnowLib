package io.github.snow1026.snowlib.commands

import org.bukkit.plugin.java.JavaPlugin
import kotlin.reflect.KClass

/**
 * A Kotlin-friendly DSL wrapper for the SnowLib Sommand framework.
 * This class provides a more idiomatic way to define commands in Kotlin.
 *
 * Example:
 * ```kotlin
 * val commands = KotlinSommand(this)
 *
 * commands.register("team") {
 * literal("create") {
 * argument("name", String::class) {
 * executes { ctx ->
 * val teamName = ctx.getArgument<String>("name")
 * ctx.sender.sendMessage("§aTeam '$teamName' created!")
 * }
 * }
 * }
 * literal("list") {
 * executes { ctx ->
 * ctx.sender.sendMessage("§eTeams: Alpha, Beta, Gamma")
 * }
 * }
 * }
 * ```
 */
class KotlinSommand(plugin: JavaPlugin) {

    private val javaSommand = Sommand(plugin)

    /**
     * Registers a new command using the Kotlin DSL.
     *
     * @param name The name of the command.
     * @param block The DSL block for configuring the [CommandNode].
     */
    fun register(name: String, block: CommandNode.() -> Unit) {
        javaSommand.register(name).apply(block)
    }

    /**
     * Provides access to the underlying Java {@link Sommand} instance for advanced configuration.
     * @return The raw Java Sommand instance.
     */
    fun java(): Sommand = javaSommand
}

/**
 * DSL helper to define a required argument.
 *
 * @param name The name of the argument.
 * @param type The KClass of the argument's type.
 * @param block A block to configure the argument's node.
 */
fun <T : Any> CommandNode.argument(name: String, type: KClass<T>, block: CommandNode.() -> Unit = {}) {
    this.argument(name, type.java, block)
}

/**
 * DSL helper to define a literal sub-command.
 *
 * @param name The name of the literal.
 * @param block A block to configure the literal's node.
 */
fun CommandNode.literal(name: String, block: CommandNode.() -> Unit = {}) {
    this.literal(name, block)
}

/**
 * DSL helper to define the execution logic for a command node.
 *
 * @param block The action to perform on execution.
 */
fun CommandNode.executes(block: (ctx: CommandContext) -> Unit) {
    this.executes(block)
}

/**
 * A convenience extension to get a non-nullable argument from the context.
 *
 * @param T The expected type of the argument.
 * @param name The name of the argument.
 * @return The parsed argument.
 * @throws IllegalArgumentException if the argument is not found.
 */
inline fun <reified T> CommandContext.getArgument(name: String): T {
    return this.getArgument(name)
}
