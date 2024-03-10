package org.cascadebot.bot.rabbitmq.consumers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.requests.RestAction
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.DiscordIDObject
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MutualGuildResponse
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode

fun ShardConsumer.onShardBroadcast(
    context: RabbitMQContext,
    body: ObjectNode
) {
    val action = context.properties.headers["action"].toString()

    val response: Any = when (action) {
        "user:mutual_guilds" -> {
            if (!assertReplyTo(context)) return
            val userId = Main.json.treeToValue<DiscordIDObject>(body).id.toLongOrNull()

            if (userId == null) {
                return CommonResponses.DISCORD_ID_INVALID.sendAndAck(context)
            }

            val user = Main.shardManager.getUserById(userId)

            if (user == null) {
                val response = RabbitMQResponse.failure(
                    StatusCode.NotFound,
                    MiscErrorCodes.UserNotFound,
                    "User cannot be found"
                )
                response.sendAndAck(context)
                return
            }

            // TODO Filter for permissions when permission system is created
            val mutualGuilds =
                jda.getMutualGuilds(user, Main.shardManager.shards.first().selfUser).filter {
                    val member = it.getMember(user) ?: return@filter false
                    member.isOwner || member.hasPermission(Permission.ADMINISTRATOR)
                }

            val restActions = mutualGuilds.map { guild -> guild.retrieveMetaData().map { guild to it } }
            val mutualGuildsWithMeta = RestAction.allOf(restActions).complete()

            mutualGuildsWithMeta.map { MutualGuildResponse.fromGuild(it.first, it.second) }
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

    val jsonResponse = Main.json.valueToTree<JsonNode>(response)

    val wrappedResponse = RabbitMQResponse.success(jsonResponse)
    wrappedResponse.sendAndAck(context)
}