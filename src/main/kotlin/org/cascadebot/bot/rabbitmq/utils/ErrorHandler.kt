package org.cascadebot.bot.rabbitmq.utils

import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.HierarchyException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.requests.ErrorResponse
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.PermissionsErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode

object ErrorHandler {

    /**
     * Utility function to call the generator function
     *
     * Used in places where "handleError" needs to be an else condition
     */
    fun handleError(
        context: RabbitMQContext,
        error: Throwable
    ) {
        handleError(context).invoke(error)
    }

    /**
     * Generator function to return an error handler for Discord errors to return the appropriate RabbitMQ response.
     *
     * @return The function to pass to .queue() as an error handler
     */
    fun handleError(
        context: RabbitMQContext
    ): (Throwable) -> Unit {
        return { throwable ->
            when (throwable) {
                is ErrorResponseException -> {
                    if (throwable.errorResponse == ErrorResponse.UNKNOWN_MESSAGE) {
                        RabbitMQResponse.failure(
                            StatusCode.BadRequest,
                            InvalidErrorCodes.InvalidUser,
                            "The specified member was not found"
                        ).sendAndAck(context)
                    }
                }

                is InsufficientPermissionException -> {
                    RabbitMQResponse.failure(
                        StatusCode.DiscordException,
                        PermissionsErrorCodes.MissingPermission,
                        "The bot is missing the permission " + throwable.permission.name + " required to do this"
                    ).sendAndAck(context)
                }

                is HierarchyException -> {
                    RabbitMQResponse.failure(
                        StatusCode.DiscordException,
                        PermissionsErrorCodes.CannotInteract,
                        "Cannot modify this object as it is higher then the bot!"
                    ).sendAndAck(context)
                }

                else -> {
                    RabbitMQResponse.failure(
                        StatusCode.ServerException,
                        MiscErrorCodes.UnexpectedError,
                        "Received an unexpected error " + throwable.javaClass.name + ": " + throwable.message
                    ).sendAndAck(context)
                }
            }
        }
    }
}
