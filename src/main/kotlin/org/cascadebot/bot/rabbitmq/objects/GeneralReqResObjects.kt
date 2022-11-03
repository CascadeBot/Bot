package org.cascadebot.bot.rabbitmq.objects

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import org.cascadebot.bot.Main
import org.cascadebot.bot.utils.RabbitMQUtil

data class RabbitMQError(
    val errorCode: ErrorCode,
    val message: String
)

data class RabbitMQResponse<T> constructor(
    val statusCode: StatusCode,
    val data: T?,
    val error: RabbitMQError?
) {

    val isError by lazy { error != null }

    val isData by lazy { data != null }

    companion object {

        fun <T : IRMQResponse> success(data: T) = RabbitMQResponse(StatusCode.Success, data, null)

        fun <T : IRMQResponse> success(data: List<T>) = RabbitMQResponse(StatusCode.Success, data, null)

        fun success(jsonObj: JsonNode) = RabbitMQResponse(StatusCode.Success, jsonObj, null)

        fun success(key: String, value: Any?): RabbitMQResponse<ObjectNode> {
            val node = Main.json.createObjectNode()
            node.putPOJO(key, value)
            return RabbitMQResponse(StatusCode.Success, node, null)
        }

        fun success() = RabbitMQResponse(StatusCode.Success, null, null)

        fun failure(statusCode: StatusCode, errorCode: ErrorCode, message: String): RabbitMQResponse<Nothing> {
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

/*
 * Marker interfaces to mark objects which are designed to be used for response/request logic
 */
interface IRMQResponse
interface IRMQRequest