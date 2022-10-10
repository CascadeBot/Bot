package org.cascadebot.bot.rabbitmq.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

@Serializable
data class RabbitMQError(
    @SerialName("error_code") val errorCode: ErrorCode,
    val message: String
)

@Serializable
data class RabbitMQResponse<T : Any> private constructor(
    @SerialName("status_code") val statusCode: StatusCode,
    val data: T?,
    val error: RabbitMQError?
) {

    companion object {

        fun <T : Any> success(data: T): RabbitMQResponse<T> {
            return RabbitMQResponse(StatusCode.Success, data, null)
        }

        fun failure(statusCode: StatusCode, errorCode: ErrorCode, message: String): RabbitMQResponse<Unit> {
            return RabbitMQResponse(statusCode, null, RabbitMQError(errorCode, message))
        }
    }

    fun toJsonString(): String {
        return Json.encodeToString(this)
    }

    fun toJsonByteArray(): ByteArray {
        return toJsonString().toByteArray(StandardCharsets.UTF_8)
    }
}
