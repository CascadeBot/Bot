package org.cascadebot.bot.cmd.meta

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.Result
import org.apache.commons.lang3.reflect.ConstructorUtils
import org.reflections.Reflections
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

class CommandManager {

    private val _commands: MutableMap<CommandPath, ExecutableCommand> = mutableMapOf()
    val commands
        get() = _commands.toList()

    private val registeredCommands: AtomicBoolean = AtomicBoolean(false)

    fun getCommand(path: String): ExecutableCommand? {
        val parts = path.split("/")
        return _commands.filter { entry -> parts == entry.key.path }.map { it.value }.firstOrNull()
    }

    companion object {

        private val logger by SLF4J("CommandManager")
    }

    init {
        try {
            val timeElapsed = measureTimeMillis {
                val commandReflections = Reflections("org.cascadebot.bot.cmd")
                for (clazz in commandReflections.getSubTypesOf(ExecutableCommand::class.java)) {
                    if (Modifier.isAbstract(clazz.modifiers)) continue
                    val command: ExecutableCommand = ConstructorUtils.invokeConstructor(clazz) as ExecutableCommand
                    _commands[CommandPath(listOf(command.name))] = command
                }
            }

            logger.info("Loaded {} commands in {}ms.", _commands.size, timeElapsed)
        } catch (e: Exception) {
            logger.error("Could not load commands!", e)
            exitProcess(1)
        }
    }

    fun registerCommandsOnce(jda: JDA) {
        logger.debug("Beginning to register commands with Discord")

        // Already registered commands on another shard
        if (registeredCommands.getAndSet(true)) return logger.debug("Commands have already been registered on another shard")

        val startTime = System.currentTimeMillis()
        val commandActions: MutableList<RestAction<Pair<String, Result<Command>>>> = mutableListOf()
        for ((path, command) in _commands.mapKeys { it.key.path.joinToString("/") }) {
            // Each rest action returns a pair of the command path and a result, either failure or success
            // Allows printing error messages once processed
            commandActions += jda.upsertCommand(command.commandData)
                .map { Pair(path, Result.success(it)) }
                .onErrorMap { Pair(path, Result.failure(it)) }
        }
        val combinedRestAction = RestAction.allOf(commandActions)
        combinedRestAction.queue { pairs ->
            val successful = pairs.count { it.second.isSuccess }
            pairs.forEach { (path, result) ->
                if (result.isSuccess) {
                    val cmd = result.get()
                    logger.debug("Registered command '${path}' with Discord successfully. ID: ${cmd.id}")
                } else {
                    logger.error("Could not register command '${path}'", result.failure)
                }
            }
            val duration = System.currentTimeMillis() - startTime
            logger.info("Successfully registered $successful/${_commands.size} commands with Discord in ${duration}ms")
        }

    }
}