package org.cascadebot.bot.rabbitmq.consumers

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.Channel
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.actions.ActionProcessors
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import java.util.Optional

class ShardConsumer(channel: Channel, private val shardId: Int, internal val jda: JDA) : ErrorHandledConsumer(channel) {

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

        // If this is a broadcast request, send to a separate processor
        if (context.envelope.routingKey == "shard.all") {
            onShardBroadcast(
                context,
                jsonBody
            )
            return
        }

        val action = context.properties.headers["action"].toString()

        val consumerEnum = ActionProcessors.entries.firstOrNull { action.startsWith(it.root) }

        if (consumerEnum == null) {
            CommonResponses.UNSUPPORTED_ACTION.sendAndAck(context)
            return
        }

        val guildResult = validateGuildId(jsonBody, context)

        if (guildResult.isEmpty) return

        val actionParts = action.removePrefix(consumerEnum.root + ":") // Remove the prefix followed by its colon
            .split(":")
            .filter { it.isNotBlank() } // Remove any blank sections or empty sections caused by ::

        consumerEnum.processor.consume(
            actionParts,
            jsonBody,
            context,
            guildResult.get()
        )?.sendAndAck(context)
    }

    private fun validateGuildId(jsonBody: ObjectNode, context: RabbitMQContext): Optional<Guild> {
        val guildIdField = jsonBody.get("guild_id")

        if (guildIdField == null) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidGuild,
                "Guild must be specified in the request"
            ).sendAndAck(context)
            return Optional.empty()
        }

        val guildId = guildIdField.asText().toLongOrNull()

        if (guildId == null) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidGuild,
                "Guild ID must be a 64-bit int contained in a string"
            ).sendAndAck(context)
            return Optional.empty()
        }

        val guildShardId = ((guildId shr 22) % Main.shardManager.shardsTotal).toInt()

        if (guildShardId != shardId) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidShard,
                "Shard mismatch. Guild shard: $shardId. Requested shard: $shardId"
            ).sendAndAck(context)
            return Optional.empty()
        }

        val guild = jda.getGuildById(guildId)

        if (guild == null) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidGuild,
                "The specified guild does not exist"
            ).sendAndAck(context)
            return Optional.empty()
        }

        return Optional.of(guild)
    }

}