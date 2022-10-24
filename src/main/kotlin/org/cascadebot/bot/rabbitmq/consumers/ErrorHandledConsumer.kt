package org.cascadebot.bot.rabbitmq.consumers

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import dev.minn.jda.ktx.util.SLF4J
import org.cascadebot.bot.rabbitmq.objects.ErrorCode
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode

abstract class ErrorHandledConsumer(channel: Channel) : DefaultConsumer(channel) {

    val logger by SLF4J

    override fun handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        properties: BasicProperties,
        body: ByteArray
    ) {
        try {
            onDeliver(consumerTag, envelope, properties, body.decodeToString())
        } catch (e: Exception) {
            logger.error(
                "Error trying to send error message (${e.javaClass.simpleName}): " + (e.message ?: "<No Message>")
            )
            // If there's no queue to reply to, we can't tell the client what's up!
            if (properties.replyTo == null) return
            // As it's unlikely we'll be able to respond to a client with an IOException, we'll try but fast fail
            try {
                val response = RabbitMQResponse.failure(
                    StatusCode.ServerException,
                    ErrorCode.fromException(e),
                    e.message ?: e.javaClass.simpleName
                )
                response.sendAndAck(channel, properties, envelope)
            } catch (e: Exception) {
                logger.error(
                    "Error trying to send error message (${e.javaClass.simpleName}): " + (e.message ?: "<No Message>")
                )
                // We tried, and we failed :(
                return
            }
        }
    }

    internal fun assertReplyTo(properties: BasicProperties, envelope: Envelope): Boolean {
        if (properties.replyTo == null) {
            channel.basicReject(envelope.deliveryTag, false)
            return false
        }
        return true
    }

    abstract fun onDeliver(
        consumerTag: String,
        envelope: Envelope,
        properties: BasicProperties,
        body: String
    )

}