package org.cascadebot.bot.rabbitmq.consumers

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.actions.Consumers
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode

class ShardConsumer(channel: Channel, private val shardId: Int, internal val jda: JDA) : ErrorHandledConsumer(channel) {

    override fun onDeliver(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: String) {
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

        val action = properties.headers["action"].toString()

        val consumerEnum = Consumers.values().firstOrNull { action.startsWith(it.root) }

        if (consumerEnum == null) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            ).sendAndAck(channel, properties, envelope)
            return
        }

        if (consumerEnum.requiresGuild) {
            if (!validateGuildId(jsonBody, properties, envelope)) return
        }

        val actionParts = action.removePrefix(consumerEnum.root).split(":")

        consumerEnum.consumer.consume(
            actionParts,
            jsonBody,
            envelope,
            properties,
            channel,
            jda
        )?.sendAndAck(channel, properties, envelope)
    }

    private fun validateGuildId(jsonBody: ObjectNode, properties: BasicProperties, envelope: Envelope): Boolean {
        val guildIdField = jsonBody.get("guild_id")

        if (guildIdField == null) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidGuild,
                "Guild must be specified in the request"
            ).sendAndAck(channel, properties, envelope)
            return false
        }

        val guildId = guildIdField.asText().toLongOrNull()

        if (guildId == null) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidGuild,
                "Guild ID must be a 64-bit int contained in a string"
            ).sendAndAck(channel, properties, envelope)
            return false
        }

        val guildShardId = ((guildId shr 22) % Main.shardManager.shardsTotal).toInt()

        if (guildShardId != shardId) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidShard,
                "Shard mismatch. Guild shard: $shardId. Requested shard: $shardId"
            ).sendAndAck(channel, properties, envelope)
            return false
        }

        if (jda.getGuildById(guildId) == null) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidGuild,
                "The specified guild does not exist"
            ).sendAndAck(channel, properties, envelope)
            return false
        }

        return true
    }

}