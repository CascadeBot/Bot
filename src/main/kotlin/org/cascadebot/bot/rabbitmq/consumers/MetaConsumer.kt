package org.cascadebot.bot.rabbitmq.consumers

import com.fasterxml.jackson.databind.node.IntNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode

class MetaConsumer(channel: Channel) : ErrorHandledConsumer(channel) {

    override fun onDeliver(
        consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        body: String
    ) {
        if (!assertReplyTo(properties, envelope)) return

        val response = Main.json.createObjectNode()

        val property = properties.headers["action"].toString()

        when (property) {
            "shard_count" -> {
                response.set("shard_count", IntNode(Main.shardManager.shardsTotal))
            }

            else -> {
                val responseObject = RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidProperty,
                    "The property '$property' is invalid"
                )
                responseObject.sendAndAck(channel, properties, envelope)
                return
            }
        }

        val responseObject = RabbitMQResponse.success(response)
        responseObject.sendAndAck(channel, properties, envelope)
    }

}