package org.cascadebot.bot

import ch.qos.logback.classic.Level
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.ApplicationInfo
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.cascadebot.bot.cmd.meta.CommandManager
import org.cascadebot.bot.components.ComponentCache
import org.cascadebot.bot.db.PostgresManager
import org.cascadebot.bot.events.InteractionListener
import org.cascadebot.bot.events.ReadyListener
import org.cascadebot.bot.rabbitmq.RabbitMQManager
import org.cascadebot.bot.utils.ColorDeserializer
import org.cascadebot.bot.utils.ColorSerializer
import org.cascadebot.bot.utils.LogbackUtil
import org.cascadebot.bot.utils.addModule
import org.hibernate.HibernateException
import java.awt.Color
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

object Main {

    val logger by SLF4J("Main")

    lateinit var componentCache: ComponentCache
        private set

    val interactionHookCache: Cache<String, InteractionHook> =
        Caffeine.newBuilder().expireAfterWrite(20, TimeUnit.MINUTES).build()

    lateinit var shardManager: ShardManager
        private set

    lateinit var applicationInfo: ApplicationInfo
        private set

    lateinit var commandManager: CommandManager
        private set

    lateinit var postgres: PostgresManager
        private set

    var rabbitMQManager: RabbitMQManager? = null
        private set

    lateinit var config: Config
        private set

    val json: JsonMapper = jsonMapper {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)

        addModule(kotlinModule())
        addModule {
            addSerializer(Color::class.java, ColorSerializer())
            addDeserializer(Color::class.java, ColorDeserializer())
        }
    }

    private fun runBot() {
        logger.info("Starting CascadeBot")

        config = loadConfig()

        if (config.development?.debugLogs == true) {
            LogbackUtil.setAppenderLevel("STDOUT", Level.DEBUG)
        }

        componentCache = ComponentCache(config.values.maxComponentsCachedPerChannel)

        try {
            postgres = PostgresManager(config.database)
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
        applicationInfo = shardManager.retrieveApplicationInfo().complete()
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