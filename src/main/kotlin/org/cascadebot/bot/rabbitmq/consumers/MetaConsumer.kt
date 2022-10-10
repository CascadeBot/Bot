package org.cascadebot.bot.rabbitmq.consumers

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
        val response = mutableMapOf<String, JsonElement>()

        val property = properties.headers["action"].toString()

        when (property) {
            "shard-count" -> {
                response["shard-count"] = JsonPrimitive(Main.shardManager.shardsTotal)
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

        val responseObject = RabbitMQResponse.success(JsonObject(response))
        responseObject.sendAndAck(channel, properties, envelope)
    }

}