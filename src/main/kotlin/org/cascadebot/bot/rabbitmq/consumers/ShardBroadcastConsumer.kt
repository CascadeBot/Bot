package org.cascadebot.bot.rabbitmq.consumers

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MutualGuildResponse
import org.cascadebot.bot.rabbitmq.objects.NotFoundErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.objects.UserIDObject

class ShardBroadcastConsumer(val jda: JDA, channel: Channel) : ErrorHandledConsumer(channel) {

    override fun onDeliver(
        consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        body: String
    ) {
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

        val response: Any = when (action) {
            "user:mutual_guilds" -> {
                if (!assertReplyTo(properties, envelope)) return
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

                // TODO Filter for permissions when permission system is created
                val mutualGuilds =
                    jda.getMutualGuilds(user, Main.shardManager.shards.first().selfUser).filter {
                        val member = it.getMember(user) ?: return@filter false
                        member.isOwner || member.hasPermission(Permission.ADMINISTRATOR)
                    }

                mutualGuilds.map { MutualGuildResponse.fromGuild(it) }
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