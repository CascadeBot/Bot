package org.cascadebot.bot.cmd.meta

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import org.apache.commons.lang3.reflect.ConstructorUtils
import org.reflections.Reflections
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log
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
                    _commands[CommandPath(listOf(command.name))] = command;
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
        if (registeredCommands.get()) return logger.debug("Commands have already been registered on another shard")

        var successful = 0
        val elapsedTime = measureTimeMillis {
            for ((path, command) in _commands) {
                logger.debug("Attempting to register command '$path'")
                try {
                    jda.upsertCommand(command.commandData).complete()
                    successful++
                } catch (e: RuntimeException) {
                    logger.error("Could not register command '$path'", e)
                }
            }
        }

        logger.info("Successfully registered $successful/${_commands.size} commands with Discord in ${elapsedTime}ms")
    }
}