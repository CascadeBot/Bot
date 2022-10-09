package org.cascadebot.bot.rabbitmq.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets


/*
    Status Codes

    0   -  99   Success
    100 - 199   Data Error
    200 - 299   Server Error
    300 - 399   Scripting Error
    400 - 499   ?

 */
@Serializable
@JvmInline
value class StatusCode private constructor(val code: Int) {

    companion object {

        val Success = StatusCode(0)
        val NotFound = StatusCode(100)
        val BadRequest = StatusCode(101)

        val ServerException = StatusCode(200)

    }

}

@Serializable
@JvmInline
value class ErrorCode private constructor(val code: String) {

    companion object {

        val UserNotFound = ErrorCode("user_not_found")
        val InvalidProperty = ErrorCode("invalid_property")
        val InvalidMethod = ErrorCode("invalid_method")

        val InvalidJsonFormat = ErrorCode("invalid_json")

        fun fromException(e: Exception): ErrorCode {
            return ErrorCode(e.javaClass.simpleName)
        }

    }


}

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
