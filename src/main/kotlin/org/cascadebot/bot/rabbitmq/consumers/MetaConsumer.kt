package org.cascadebot.bot.rabbitmq.consumers

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.ErrorCode
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode

class MetaConsumer(channel: Channel) : DefaultConsumer(channel) {

    override fun handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        body: ByteArray
    ) {
        val replyProps = AMQP.BasicProperties.Builder()
            .correlationId(properties.correlationId)
            .build()

        val response = mutableMapOf<String, JsonElement>()
        when (val property = properties.headers["property"].toString()) {
            "shard-count" -> {
                response["shard-count"] = JsonPrimitive(Main.shardManager.shardsTotal)
            }

            else -> {
                val responseObject = RabbitMQResponse.failure(StatusCode.BadRequest, ErrorCode.InvalidProperty, "The property '$property' is invalid")
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