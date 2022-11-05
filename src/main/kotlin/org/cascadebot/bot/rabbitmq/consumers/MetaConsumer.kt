package org.cascadebot.bot.rabbitmq.consumers

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.utils.createJsonObject

class MetaConsumer(channel: Channel) : ErrorHandledConsumer(channel) {

    override fun onDeliver(
        consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        body: String
    ) {
        if (!assertReplyTo(properties, envelope)) return

        val responseProps = mutableListOf<Pair<String, Any?>>()

        val property = properties.headers["action"].toString()

        when (property) {
            "shard_count" -> {
                responseProps.add("shard_count" to Main.shardManager.shardsTotal)
            }

            else -> {
                RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidProperty,
                    "The property '$property' is invalid"
                ).sendAndAck(channel, properties, envelope)
                return
            }
        }

        val responseObject = RabbitMQResponse.success(createJsonObject(responseProps))
        responseObject.sendAndAck(channel, properties, envelope)
    }

}