package org.cascadebot.bot.rabbitmq.consumers

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.ErrorCode
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.utils.RabbitMQUtil

class BroadcastConsumer(channel: Channel) : DefaultConsumer(channel) {

    val logger by SLF4J

    override fun handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        body: ByteArray
    ) {
        val replyProps = RabbitMQUtil.propsFromCorrelationId(properties)

        val jsonBody = try {
            Json.decodeFromString<JsonObject>(body.decodeToString())
        } catch (e: Exception) {
            logger.error("Could not decode RabbitMQ message", e)
            channel.basicReject(envelope.deliveryTag, false)
            return
        }

        val method = properties.headers["method"].toString()

        val response: Any = when (method) {
            "user.mutual_guilds" -> {
                val userId = (jsonBody["user_id"] as? JsonPrimitive)?.content?.toLongOrNull()

                if (userId == null) {
                    val response = RabbitMQResponse.failure(StatusCode.NotFound, ErrorCode.UserNotFound, "User cannot be found")
                    channel.basicPublish("", properties.replyTo, replyProps, response.toJsonByteArray())
                    channel.basicAck(envelope.deliveryTag, false)
                    return
                }

                val user = Main.shardManager.getUserById(userId) ?: return

                // TODO Filter for permissions when permission system is created
                val mutualGuilds =
                    Main.shardManager.getMutualGuilds(user, Main.shardManager.shards.first().selfUser).filter {
                        val member = it.getMember(user) ?: return@filter false
                        member.isOwner || member.hasPermission(Permission.ADMINISTRATOR)
                    }

                mutualGuilds.map { MutualGuildResponse(it) }
            }

            else -> {
                val response = RabbitMQResponse.failure(StatusCode.BadRequest, ErrorCode.InvalidMethod, "The method '$method' is invalid.")
                channel.basicPublish("", properties.replyTo, replyProps, response.toJsonByteArray())
                channel.basicAck(envelope.deliveryTag, false)
                return
            }
        }

        val wrappedResponse = RabbitMQResponse.success(response)

        channel.basicPublish("", properties.replyTo, replyProps, wrappedResponse.toJsonByteArray())
        channel.basicAck(envelope.deliveryTag, false)
    }
}

@Serializable
data class MutualGuildResponse(
    @SerialName("guild_id") val guildId: Long,
    val name: String,
    @SerialName("icon_url") val iconUrl: String?
) {

    constructor(guild: Guild) : this(guild.idLong, guild.name, guild.iconUrl)
}