package org.cascadebot.bot

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.cascadebot.bot.db.PostgresManager
import org.cascadebot.bot.events.ReadyListener

class Main {

    val logger by SLF4J("Main")
    var shardManager: ShardManager
    var postgresManager: PostgresManager
    init {
        logger.info("Starting CascadeBot")

        val config = readConfig()

        val defaultShardManagerBuilder = DefaultShardManagerBuilder.create(GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS)) // TODO do we want to have all here? I imagine eventually, but don't know about mvp
            .setToken(config.botConfig.token)
            .setActivityProvider { Activity.playing("Cascade Bot") }
            .addEventListeners(ReadyListener(this))

        val shardConfig: Sharding? = config.botConfig.shards
        if (shardConfig != null) {
            when (shardConfig) {
                is Sharding.Total -> {
                    defaultShardManagerBuilder.setShardsTotal(shardConfig.total)
                }
                is Sharding.MinMax -> {
                    defaultShardManagerBuilder.setShardsTotal(shardConfig.total)
                    defaultShardManagerBuilder.setShards(shardConfig.min, shardConfig.max)
                }
            }
        }

        shardManager = defaultShardManagerBuilder.build()

        postgresManager = PostgresManager(config.database)

    }
}

fun main() {
    Main()
}