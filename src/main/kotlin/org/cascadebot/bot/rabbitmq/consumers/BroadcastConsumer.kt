package org.cascadebot.bot.rabbitmq.consumers

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.NotFoundErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.objects.UserIDObject

class BroadcastConsumer(channel: Channel) : ErrorHandledConsumer(channel) {

    override fun onDeliver(
        consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        body: String
    ) {
        val jsonBody = try {
            Json.decodeFromString<JsonObject>(body)
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
            "user.mutual_guilds" -> {
                val decodeResult = kotlin.runCatching { Json.decodeFromJsonElement<UserIDObject>(jsonBody) }

                if (decodeResult.isFailure) {
                    RabbitMQResponse.failure(
                        StatusCode.BadRequest,
                        InvalidErrorCodes.InvalidRequestFormat,
                        "Invalid request format, please specify user_id"
                    ).sendAndAck(channel, properties, envelope)
                }

                val userId = decodeResult.getOrNull()?.userId

                if (userId == null) {
                    val response = RabbitMQResponse.failure(
                        StatusCode.NotFound,
                        NotFoundErrorCodes.UserNotFound,
                        "User cannot be found"
                    )
                    response.sendAndAck(channel, properties, envelope)
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

@Serializable
data class MutualGuildResponse(
    @SerialName("guild_id") val guildId: Long,
    val name: String,
    @SerialName("icon_url") val iconUrl: String?
) {

    constructor(guild: Guild) : this(guild.idLong, guild.name, guild.iconUrl)
}