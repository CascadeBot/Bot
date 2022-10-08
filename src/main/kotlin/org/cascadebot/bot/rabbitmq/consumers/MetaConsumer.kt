package org.cascadebot.bot.rabbitmq.consumers

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.cascadebot.bot.Main

class MetaConsumer(private val channel: Channel) : DeliverCallback {

    override fun handle(consumerTag: String, message: Delivery) {
        val replyProps = AMQP.BasicProperties.Builder()
            .correlationId(message.properties.correlationId)
            .build()

        val response = mutableMapOf<String, JsonElement>()
        when (message.envelope.routingKey) {
            "meta.shard-count" -> {
                response["shard-count"] = JsonPrimitive(Main.shardManager.shardsTotal)
            }
        }

        val json = Json.encodeToString(JsonObject(response))

        channel.basicPublish("", message.properties.replyTo, replyProps, json.toByteArray());
        channel.basicAck(message.envelope.deliveryTag, false);
    }
}