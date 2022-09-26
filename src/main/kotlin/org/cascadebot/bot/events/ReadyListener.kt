package org.cascadebot.bot.events

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.cascadebot.bot.Main

class ReadyListener(): ListenerAdapter() {

    private val logger by SLF4J

    override fun onReady(event: ReadyEvent) {
        val shardId = event.jda.shardInfo.shardId
        logger.info("JDA shard $shardId logged in!") // TODO this is here to test that jda is working, we probably don't need this

        val rabbitMQManager = Main.rabbitMQManager
        if (rabbitMQManager != null) {
            // This serves 2 purposes, logging and also pre-emptively creating a channel for each shard
            logger.info("Opened RabbitMQ channel number '{}' for shard ID '{}'", rabbitMQManager.channel.channelNumber,
                shardId
            )

            rabbitMQManager.channel.queueDeclare("shard-$shardId", true, false, false, mapOf())
            rabbitMQManager.channel.queueBind("shard-$shardId", "amq.topic", "shard.$shardId.*.#")
        }
    }
}