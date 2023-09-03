package org.cascadebot.bot.rabbitmq.objects

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.AMQP.Exchange
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.impl.AMQImpl
import org.cascadebot.bot.Main
import org.cascadebot.bot.RabbitMQ
import org.cascadebot.bot.utils.RabbitMQUtil
import org.cascadebot.bot.utils.createJsonObject

data class RabbitMQError(
    val errorCode: ErrorCode,
    val message: String
)

data class RabbitMQResponse<T>(
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
            val obj = createJsonObject(
                key to value
            )
            return RabbitMQResponse(StatusCode.Success, obj, null)
        }

        fun success() = RabbitMQResponse(StatusCode.Success, null, null)

        fun failure(statusCode: StatusCode, errorCode: ErrorCode, message: String): RabbitMQResponse<Nothing> {
            return RabbitMQResponse(statusCode, null, RabbitMQError(errorCode, message))
        }
    }

    private fun toJsonByteArray(): ByteArray {
        return Main.json.writeValueAsBytes(this)
    }

    /**
     * Publishes the [RabbitMQResponse] using the specified [channel] and [properties]. Also acknowledges the original
     * RabbitMQ message using the specified [envelope].
     *
     * Converts the response object into a UTF-8 byte representation of the JSON serialisation.
     * The JSON serialisation follows the structure as follows:
     * ```
     * {
     *  statusCode: number
     *  data?: T
     *  error?: RabbitMQError
     * }
     * ```
     *
     * @param channel The RabbitMQ channel over which the response will be sent.
     * @param properties The received properties used to determine where to send the response.
     * @param envelope The RabbitMQ envelope which contains the delivery tag to acknowledge.
     */
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

/**
 * Marker interface that is used to mark a class as suitable to be used as a response over RabbitMQ.
 *
 * The [RabbitMQResponse] construction methods only access [IRMQResponse] or raw JSON and string data
 */
interface IRMQResponse

/**
 * Marker interface that is used to mark a class as a request that is received over RabbitMQ.
 */
interface IRMQRequest

class RabbitMQException(val response: RabbitMQResponse<Nothing>) : Exception(response.error!!.message) {

    init {
        if (!response.isError) {
            throw IllegalArgumentException("Exception cannot be used for non-error responses")
        }
    }

}