package org.cascadebot.bot

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.cascadebot.bot.db.PostgresManager
import org.cascadebot.bot.events.ReadyListener
import kotlin.system.exitProcess

object Main {
    val logger by SLF4J("Main")

    lateinit var shardManager: ShardManager
        private set
    lateinit var postgresManager: PostgresManager
        private set
    lateinit var config: Config
        private set

    private fun runBot() {
        logger.info("Starting CascadeBot")

        config = loadConfig()

        shardManager = buildShardManager()

        postgresManager = PostgresManager(config.database)
    }

    private fun loadConfig(): Config {
        val configResult = Config.load()
        if (configResult.isInvalid()) {
            // Print out the invalid config message. Replace all double new lines with single for compactness
            logger.error("Could not load config: \n" + configResult.getInvalidUnsafe().description().replace("\n\n", "\n"))
            exitProcess(1)
        }

        return configResult.getUnsafe()
    }

    private fun buildShardManager(): ShardManager {
        val defaultShardManagerBuilder = DefaultShardManagerBuilder.create(GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS)) // TODO do we want to have all here? I imagine eventually, but don't know about mvp
            .setToken(config.botConfig.token)
                .setToken(config.discord.token)
            .addEventListeners(ReadyListener(this))

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

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            runBot()
        } catch (e: Exception) {
            logger.error("Error in bot execution", e)
        }
    }
}