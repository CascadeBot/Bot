package org.cascadebot.bot.rabbitmq.consumers

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.actions.Consumers
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode

class ShardConsumer(channel: Channel, private val shardId: Int, internal val jda: JDA) : ErrorHandledConsumer(channel) {

    override fun onDeliver(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: String) {
        if (!assertReplyTo(properties, envelope)) return

        val jsonBody = try {
            Main.json.readValue(body, ObjectNode::class.java)
        } catch (e: Exception) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidJsonFormat,
                e.message ?: e.javaClass.simpleName
            ).sendAndAck(channel, properties, envelope)
            return
        }

        // If this is a broadcast request, send to a separate processor
        if (envelope.routingKey == "shard.all") {
            onShardBroadcast(
                consumerTag,
                envelope,
                properties,
                jsonBody
            )
            return
        }

        if (jsonBody.has("guild")) {
            val guildId = try {
                jsonBody.get("guild").asText().toLong()
            } catch (e: NumberFormatException) {
                RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidGuild,
                    e.message ?: e.javaClass.simpleName
                ).sendAndAck(channel, properties, envelope)
                return
            }

            val guildShardId = ((guildId shr 22) % Main.shardManager.shardsTotal).toInt()

            if (guildShardId != shardId) {
                RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidShard,
                    "Shard mismatch. Guild shard: $shardId. Requested shard: $shardId"
                ).sendAndAck(channel, properties, envelope)
                return
            }

            if (jda.getGuildById(guildId) == null) {
                RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidGuild,
                    "The specified guild does not exist"
                ).sendAndAck(channel, properties, envelope)
                return
            }
        }

        val action = properties.headers["action"].toString()


        val consumerEnum: Consumers = try {
            Consumers.values().first { action.startsWith(it.root) }
        } catch (e: NoSuchElementException) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            ).sendAndAck(channel, properties, envelope)
            return
        }

        action.removePrefix(consumerEnum.root)

        val actionParts = action.split(":")

        consumerEnum.consumer.consume(
            actionParts,
            jsonBody,
            envelope,
            properties,
            channel,
            shardId
        )?.sendAndAck(channel, properties, envelope)
    }

}