package org.cascadebot.bot.rabbitmq.consumers

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.rabbitmq.client.Channel
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.DiscordIDObject
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.objects.UserResponse

class ResourceConsumer(channel: Channel) : ErrorHandledConsumer(channel) {

    override fun onDeliver(consumerTag: String, context: RabbitMQContext, body: String) {
        if (!assertReplyTo(context)) return

        val jsonBody = Main.json.readValue(body, ObjectNode::class.java)

        val action = context.properties.headers["action"].toString()

        val response = when (action) {
            "user:get_by_id" -> {
                val userId = Main.json.treeToValue<DiscordIDObject>(jsonBody).id.toLongOrNull()

                if (userId == null) {
                    return CommonResponses.DISCORD_ID_INVALID.sendAndAck(context)
                }

                val user = Main.shardManager.retrieveUserById(userId).complete()

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