package org.cascadebot.bot.rabbitmq.consumers

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import org.cascadebot.bot.Main

class BroadcastConsumer(private val channel: Channel) : DeliverCallback {

    val logger by SLF4J

    override fun handle(consumerTag: String, message: Delivery) {
        val replyProps = AMQP.BasicProperties.Builder()
            .correlationId(message.properties.correlationId)
            .build()

        val jsonBody = try {
            Json.decodeFromString<JsonObject>(message.body.decodeToString())
        } catch (e: Exception) {
            logger.error("Could not decode RabbitMQ message", e)
            channel.basicReject(message.envelope.deliveryTag, false)
            return
        }



        var response: Any = when (message.properties.headers["method"].toString()) {
            "user.mutual_guilds" -> {
                // TODO: Better error handling here
                val userId = (jsonBody["user_id"] as? JsonPrimitive)?.content?.toLongOrNull()

                if (userId == null) {
                    channel.basicReject(message.envelope.deliveryTag, false)
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
                channel.basicReject(message.envelope.deliveryTag, false)
                return
            }
        }

        // TODO: Reply with response
    }
}

@Serializable
data class MutualGuildResponse(
    @SerialName("guild_id") val guildId: Long,
    val name: String,
    @SerialName("icon_url") val iconUrl: String?) {

    constructor(guild: Guild) : this(guild.idLong, guild.name, guild.iconUrl)
}