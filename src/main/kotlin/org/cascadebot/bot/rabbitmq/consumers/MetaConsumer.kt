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
import org.cascadebot.bot.utils.RabbitMQUtil

class MetaConsumer(channel: Channel) : ErrorHandledConsumer(channel) {

    override fun onDeliver(
        consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        body: String
    ) {
        val replyProps = RabbitMQUtil.propsFromCorrelationId(properties)

        val response = mutableMapOf<String, JsonElement>()
        when (val property = properties.headers["property"].toString()) {
            "shard-count" -> {
                response["shard-count"] = JsonPrimitive(Main.shardManager.shardsTotal)
            }

            else -> {
                val responseObject = RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidProperty,
                    "The property '$property' is invalid"
                )
                channel.basicPublish("", properties.replyTo, replyProps, responseObject.toJsonByteArray())
                channel.basicAck(envelope.deliveryTag, false)
                return
            }
        }

        val responseObject = RabbitMQResponse.success(JsonObject(response))

        channel.basicPublish("", properties.replyTo, replyProps, responseObject.toJsonByteArray());
        channel.basicAck(envelope.deliveryTag, false);
    }

}