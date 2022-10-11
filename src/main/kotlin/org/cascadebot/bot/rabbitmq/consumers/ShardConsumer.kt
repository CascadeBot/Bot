package org.cascadebot.bot.rabbitmq.consumers

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.actions.Consumers
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import java.lang.NumberFormatException
import java.util.NoSuchElementException

class ShardConsumer(channel: Channel, private val shard: Int) : ErrorHandledConsumer(channel) {

    override fun onDeliver(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: String) {
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

            val shardId = ((guildId shr 22) % Main.shardManager.shardsTotal).toInt();

            if (shardId != shard) {
                RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidShard,
                    "Shard mismatch. Guild shard: $shardId. Requested shard: $shard"
                ).sendAndAck(channel, properties, envelope)
                return
            }

            if (Main.shardManager.getShardById(shardId) == null) {
                // This should technically never happen, but if it does, here we go
                RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidShard,
                    "This bot node is not responsible for this shard!"
                ).sendAndAck(channel, properties, envelope)
                logger.error("Got request for a shard this node does not control!")
                return
            }

            if (Main.shardManager.getShardById(shardId)!!.getGuildById(guildId) == null) {
                RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidGuild,
                    "The specified guild does not exist"
                ).sendAndAck(channel, properties, envelope)
                return
            }
        }

        val action = properties.headers["action"].toString()
        val actionParts = action.split(":")

        val root = actionParts[0]
        val consumerEnum: Consumers = try {
            Consumers.values().first { it.root == root };
        } catch (e: NoSuchElementException) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            ).sendAndAck(channel, properties, envelope)
            return
        }

        consumerEnum.consumer.consume(actionParts.subList(1, actionParts.size), jsonBody, envelope, properties, channel, shard);
    }

}