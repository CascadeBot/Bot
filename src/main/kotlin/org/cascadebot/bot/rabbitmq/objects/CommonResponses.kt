package org.cascadebot.bot.rabbitmq.objects

object CommonResponses {

    val UNSUPPORTED_ACTION = RabbitMQResponse.failure(
        StatusCode.BadRequest,
        InvalidErrorCodes.InvalidAction,
        "The specified action is not supported"
    )

    val CHANNEL_NOT_FOUND = RabbitMQResponse.failure(
        StatusCode.NotFound,
        MiscErrorCodes.ChannelNotFound,
        "The specified channel was not found"
    )

}