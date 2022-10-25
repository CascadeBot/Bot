package org.cascadebot.bot.rabbitmq.objects

/*
    Status Codes

    0   -  99   Success
    100 - 199   Data Error
    200 - 299   Server Error
    300 - 399   Scripting Error
    400 - 499   ?

 */
@JvmInline
value class StatusCode private constructor(val code: Int) {

    companion object {

        val Success = StatusCode(0)
        val NotFound = StatusCode(100)
        val BadRequest = StatusCode(101)

        val ServerException = StatusCode(200)
        val DiscordException = StatusCode(201)

    }

}

@JvmInline
value class ErrorCode internal constructor(val code: String) {

    companion object {

        fun fromException(e: Exception): ErrorCode {
            return ErrorCode(e.javaClass.simpleName)
        }
    }
}

object InvalidErrorCodes {

    val InvalidInteraction = ErrorCode("invalid_interaction")
    val InvalidProperty = ErrorCode("invalid_property")
    val InvalidMethod = ErrorCode("invalid_method")
    val InvalidAction = ErrorCode("invalid_action")
    val InvalidGuild = ErrorCode("invalid_guild")
    val InvalidName = ErrorCode("invalid_name")
    val InvalidShard = ErrorCode("invalid_shard")
    val InvalidUser = ErrorCode("invalid_user")
    val InvalidRole = ErrorCode("invalid_role")
    val InvalidChannel = ErrorCode("invalid_channel")

    val InvalidPermission = ErrorCode("invalid_Permission")

    val InvalidJsonFormat = ErrorCode("invalid_json")
    val InvalidRequestFormat = ErrorCode("invalid_request")

    val InvalidPosition = ErrorCode("invalid_position")
}

object MiscErrorCodes {

    val InteractionTokenNotFound = ErrorCode("interaction_token_not_found")
    val UserNotFound = ErrorCode("user_not_found")
    val GuildNotFound = ErrorCode("guild_not_found")
    val ChannelNotFound = ErrorCode("channel_not_found")
    val UnexpectedError = ErrorCode("unexpected")
    val BotDoesNotOwnMessage = ErrorCode("unowned_message")
}

object PermissionsErrorCodes {

    val MissingPermission = ErrorCode("missing_permission")
    val CannotInteract = ErrorCode("cannot_interact")
}