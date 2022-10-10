package org.cascadebot.bot.rabbitmq.objects

import com.fasterxml.jackson.annotation.JsonProperty
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import org.cascadebot.bot.Main
import org.cascadebot.bot.utils.RabbitMQUtil

data class RabbitMQError(
    val errorCode: ErrorCode,
    val message: String
)

data class RabbitMQResponse<T : Any> constructor(
    val statusCode: StatusCode,
    val data: T?,
    val error: RabbitMQError?
) {

    companion object {

        inline fun <reified T : Any> success(data: T): RabbitMQResponse<T> {
            return RabbitMQResponse(StatusCode.Success, data, null)
        }

        fun failure(statusCode: StatusCode, errorCode: ErrorCode, message: String): RabbitMQResponse<Unit> {
            return RabbitMQResponse(statusCode, null, RabbitMQError(errorCode, message))
        }
    }

    private fun toJsonByteArray(): ByteArray {
        return Main.json.writeValueAsBytes(this)
    }

    fun sendAndAck(channel: Channel, properties: AMQP.BasicProperties, envelope: Envelope) {
        val replyProps = RabbitMQUtil.replyPropsFromRequest(properties)

        channel.basicPublish(
            "",
            properties.replyTo,
            replyProps,
            this.toJsonByteArray()
        )
        channel.basicAck(envelope.deliveryTag, false)
    }

}