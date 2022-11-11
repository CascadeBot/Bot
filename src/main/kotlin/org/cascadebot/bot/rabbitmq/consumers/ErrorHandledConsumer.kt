package org.cascadebot.bot.rabbitmq.consumers

import com.fasterxml.jackson.core.JsonProcessingException
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import dev.minn.jda.ktx.util.SLF4J
import org.cascadebot.bot.rabbitmq.objects.ErrorCode
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQException
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
        } catch (e: RabbitMQException) {
            // RabbitMQException is a wrapper to break out of flow and return non-critical errors to the client
            e.response.sendAndAck(channel, properties, envelope)
        } catch (e: Exception) {
            logger.error(
                "Error in consumer (${e.javaClass.simpleName}): " + (e.message ?: "<No Message>")
            )
            // If there's no queue to reply to, we can't tell the client what's up!
            if (properties.replyTo == null) return
            // As it's unlikely we'll be able to respond to a client with an IOException, we'll try but fast fail
            try {
                val response = when (e) {
                    is JsonProcessingException -> RabbitMQResponse.failure(
                        StatusCode.BadRequest,
                        MiscErrorCodes.JsonProcessingError,
                        e.message!!
                    )

                    else -> RabbitMQResponse.failure(
                        StatusCode.ServerException,
                        ErrorCode.fromException(e),
                        e.message ?: e.javaClass.simpleName
                    )
                }
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