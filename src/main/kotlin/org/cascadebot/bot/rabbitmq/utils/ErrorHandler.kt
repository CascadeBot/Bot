package org.cascadebot.bot.rabbitmq.utils

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.HierarchyException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.requests.ErrorResponse
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.PermissionsErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode

class ErrorHandler {
    companion object {
        public fun handleError(
            envelope: Envelope,
            properties: AMQP.BasicProperties,
            channel: Channel,
            throwable: Throwable
        ) {
            when (throwable) {
                is ErrorResponseException -> {
                    if (throwable.errorResponse == ErrorResponse.UNKNOWN_MESSAGE) {
                        RabbitMQResponse.failure(
                            StatusCode.BadRequest,
                            InvalidErrorCodes.InvalidUser,
                            "The specified member was not found"
                        ).sendAndAck(channel, properties, envelope)
                    }
                }

                is InsufficientPermissionException -> {
                    RabbitMQResponse.failure(
                        StatusCode.DiscordException,
                        PermissionsErrorCodes.MissingPermission,
                        "The bot is missing the permission " + throwable.permission.name + " required to do this"
                    ).sendAndAck(channel, properties, envelope)
                }

                is HierarchyException -> {
                    RabbitMQResponse.failure(
                        StatusCode.DiscordException,
                        PermissionsErrorCodes.CannotInteract,
                        "Cannot modify this object as it is higher then the bot!"
                    ).sendAndAck(channel, properties, envelope)
                }

                else -> {
                    RabbitMQResponse.failure(
                        StatusCode.ServerException,
                        MiscErrorCodes.UnexpectedError,
                        "Received an unexpected error " + throwable.javaClass.name + ": " + throwable.message
                    ).sendAndAck(channel, properties, envelope)
                }
            }
        }
    }
}