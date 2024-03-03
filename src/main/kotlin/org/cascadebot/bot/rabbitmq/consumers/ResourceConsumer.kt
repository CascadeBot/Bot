package org.cascadebot.bot.rabbitmq.consumers

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.Channel
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.objects.UserIDObject
import org.cascadebot.bot.rabbitmq.objects.UserResponse

class ResourceConsumer(channel: Channel) : ErrorHandledConsumer(channel) {

    override fun onDeliver(consumerTag: String, context: RabbitMQContext, body: String) {
        if (!assertReplyTo(context)) return

        val jsonBody = try {
            Main.json.readValue(body, ObjectNode::class.java)
        } catch (e: Exception) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidJsonFormat,
                e.message ?: e.javaClass.simpleName
            ).sendAndAck(context)
            return
        }

        val action = context.properties.headers["action"].toString()

        val response = when (action) {
            "user:get_by_id" -> {
                val decodeResult = kotlin.runCatching { Main.json.treeToValue(jsonBody, UserIDObject::class.java) }

                if (decodeResult.isFailure) {
                    RabbitMQResponse.failure(
                        StatusCode.BadRequest,
                        InvalidErrorCodes.InvalidRequestFormat,
                        "Invalid request format, please specify user_id"
                    ).sendAndAck(context)
                }

                val userId = decodeResult.getOrNull()?.userId

                val user = userId?.let { Main.shardManager.retrieveUserById(userId).complete() }

                if (user == null) {
                    val response = RabbitMQResponse.failure(
                        StatusCode.NotFound,
                        MiscErrorCodes.UserNotFound,
                        "User cannot be found"
                    )
                    response.sendAndAck(context)
                    return
                }

                UserResponse.fromUser(user)
            }

            else -> {
                val response = RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidMethod,
                    "The method '$action' is invalid."
                )
                response.sendAndAck(context)
                return
            }
        }

        val wrappedResponse = RabbitMQResponse.success(response)
        wrappedResponse.sendAndAck(context)
    }

}