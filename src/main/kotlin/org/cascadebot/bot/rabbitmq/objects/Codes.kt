package org.cascadebot.bot.rabbitmq.objects

import kotlinx.serialization.Serializable

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
value class ErrorCode internal constructor(val code: String) {

    companion object {

        fun fromException(e: Exception): ErrorCode {
            return ErrorCode(e.javaClass.simpleName)
        }
    }
}

object InvalidErrorCodes {

    val InvalidProperty = ErrorCode("invalid_property")
    val InvalidMethod = ErrorCode("invalid_method")

    val InvalidJsonFormat = ErrorCode("invalid_json")
    val InvalidRequestFormat = ErrorCode("invalid_request")
}

object NotFoundErrorCodes {

    val UserNotFound = ErrorCode("user_not_found")
}