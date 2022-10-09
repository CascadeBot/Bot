package org.cascadebot.bot.rabbitmq.consumers

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import dev.minn.jda.ktx.util.SLF4J
import org.cascadebot.bot.rabbitmq.objects.ErrorCode
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.utils.RabbitMQUtil
import java.io.IOException

abstract class ErrorHandledConsumer(channel: Channel) : DefaultConsumer(channel) {

    val logger by SLF4J

    override fun handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        body: ByteArray
    ) {
        try {
            onDeliver(consumerTag, envelope, properties, body.decodeToString())
        } catch (e: Exception) {
            if (e is IOException) {
                logger.error("IO Error on deliver callback: " + (e.message ?: "<No Message>"))
            }
            // If there's no queue to reply to, we can't tell the client what's up!
            if (properties.replyTo == null) return
            // As it's unlikely we'll be able to respond to a client with an IOException, we'll try but fast fail
            try {
                val replyProps = RabbitMQUtil.propsFromCorrelationId(properties)
                channel.basicPublish(
                    "",
                    properties.replyTo,
                    replyProps,
                    RabbitMQResponse.failure(
                        StatusCode.ServerException,
                        ErrorCode.fromException(e),
                        e.message ?: e.javaClass.simpleName
                    ).toJsonByteArray()
                )
                channel.basicAck(envelope.deliveryTag, false)
            } catch (e: Exception) {
                logger.error(
                    "Error trying to send error message (${e.javaClass.simpleName}): " + (e.message ?: "<No Message>")
                )
                // We tried, and we failed :(
                return
            }
        }
    }

    abstract fun onDeliver(
        consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        body: String
    )

}