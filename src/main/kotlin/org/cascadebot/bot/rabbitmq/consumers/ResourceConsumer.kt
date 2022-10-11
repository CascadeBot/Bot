package org.cascadebot.bot.rabbitmq.consumers

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.NotFoundErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.objects.UserIDObject
import org.cascadebot.bot.rabbitmq.objects.UserResponse

class ResourceConsumer(channel: Channel) : ErrorHandledConsumer(channel) {

    override fun onDeliver(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: String) {
        if (!assertReplyTo(properties, envelope)) return

        val jsonBody = try {
            Main.json.readValue(body, ObjectNode::class.java)
        } catch (e: Exception) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidJsonFormat,
                e.message ?: e.javaClass.simpleName
            ).sendAndAck(channel, properties, envelope)
            return
        }

        val action = properties.headers["action"].toString()

        val response = when(action) {
            "user:get_by_id" -> {
                val decodeResult = kotlin.runCatching { Main.json.treeToValue(jsonBody, UserIDObject::class.java) }

                if (decodeResult.isFailure) {
                    RabbitMQResponse.failure(
                        StatusCode.BadRequest,
                        InvalidErrorCodes.InvalidRequestFormat,
                        "Invalid request format, please specify user_id"
                    ).sendAndAck(channel, properties, envelope)
                }

                val userId = decodeResult.getOrNull()?.userId

                val user = userId?.let { Main.shardManager.getUserById(userId) }

                if (user == null) {
                    val response = RabbitMQResponse.failure(
                        StatusCode.NotFound,
                        NotFoundErrorCodes.UserNotFound,
                        "User cannot be found"
                    )
                    response.sendAndAck(channel, properties, envelope)
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
                response.sendAndAck(channel, properties, envelope)
                return
            }
        }

        val wrappedResponse = RabbitMQResponse.success(response)
        wrappedResponse.sendAndAck(channel, properties, envelope)
    }

}