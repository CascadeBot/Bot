package org.cascadebot.bot

import ch.qos.logback.classic.Level
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.cascadebot.bot.cmd.meta.CommandManager
import org.cascadebot.bot.db.PostgresManager
import org.cascadebot.bot.events.InteractionListener
import org.cascadebot.bot.events.ReadyListener
import org.cascadebot.bot.rabbitmq.RabbitMQManager
import org.cascadebot.bot.utils.LogbackUtil
import org.hibernate.HibernateException
import kotlin.system.exitProcess

object Main {

    val logger by SLF4J("Main")

    lateinit var shardManager: ShardManager
        private set
    lateinit var commandManager: CommandManager
        private set
    lateinit var postgresManager: PostgresManager
        private set
    var rabbitMQManager: RabbitMQManager? = null
    lateinit var config: Config
        private set

    private fun runBot() {
        logger.info("Starting CascadeBot")

        config = loadConfig()

        if (config.development.debugLogs) {
            LogbackUtil.setAppenderLevel("STDOUT", Level.DEBUG)
        }

        try {
            postgresManager = PostgresManager(config.database)
        } catch (e: HibernateException) {
            // Get the root Hibernate exception
            var exception = e
            while (exception.cause != null && exception.cause is HibernateException) {
                exception = e.cause as HibernateException
            }
            logger.error("Could not initialise database: {}", exception.message)
            exitProcess(1)
        }

        if (config.rabbitMQ != null) {
            rabbitMQManager = RabbitMQManager(config.rabbitMQ!!)
        } else {
            logger.warn("RabbitMQ config not detected, won't be able to communicate with any other components")
        }

        commandManager = CommandManager()

        shardManager = buildShardManager()
    }

    private fun loadConfig(): Config {
        val configResult = Config.load()
        if (configResult.isInvalid()) {
            // Print out the invalid config message. Replace all double new lines with single for compactness
            logger.error(
                "Could not load config: \n" + configResult.getInvalidUnsafe().description().replace("\n\n", "\n")
            )
            exitProcess(1)
        }

        return configResult.getUnsafe()
    }

    private fun buildShardManager(): ShardManager {
        val defaultShardManagerBuilder =
            DefaultShardManagerBuilder.create(GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS)) // TODO do we want to have all here? I imagine eventually, but don't know about mvp
                .setToken(config.discord.token)
                .setActivityProvider { Activity.playing("Cascade Bot") }
                .addEventListeners(ReadyListener())
                .addEventListeners(InteractionListener())

        config.discord.shards?.let {
            when (it) {
                is Sharding.Total -> {
                    defaultShardManagerBuilder.setShardsTotal(it.total)
                }

                is Sharding.MinMax -> {
                    defaultShardManagerBuilder.setShardsTotal(it.total)
                    defaultShardManagerBuilder.setShards(it.min, it.max)
                }
            }
        }

        return defaultShardManagerBuilder.build()
    }

    private fun updateDiscordCommands(token: String) {
        LogbackUtil.setAppenderLevel("STDOUT", Level.DEBUG)
        logger.info("Uploading commands to Discord and then exiting.")
        val cmdManager = CommandManager()
        val jda = JDABuilder.createLight(token)
            .setEnabledIntents(listOf())
            .build()
        cmdManager.registerCommandsOnce(jda)
        exitProcess(0)
    }

    private fun processCmdArgs(args: Array<String>) {
        if (args.isNotEmpty()) {
            val parser = ArgParser("example")

            val updateCommands by parser.option(
                ArgType.String,
                fullName = "update-commands",
                shortName = "u",
                description = "Attempts to update slash commands with Discord then exits",
            )

            parser.parse(args)

            updateCommands?.let { token -> updateDiscordCommands(token) }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        processCmdArgs(args)

        try {
            runBot()
        } catch (e: Exception) {
            logger.error("Error in bot execution", e)
        }
    }
}