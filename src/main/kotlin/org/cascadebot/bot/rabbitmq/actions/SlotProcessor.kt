package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import org.cascadebot.bot.Main
import org.cascadebot.bot.db.entities.AutoResponderEntity
import org.cascadebot.bot.db.entities.CustomCommandEntity
import org.cascadebot.bot.db.entities.GuildSlotEntity
import org.cascadebot.bot.db.entities.SlotEntry
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.utils.QueryUtils.getEntities
import org.cascadebot.bot.utils.QueryUtils.queryEntity
import org.cascadebot.bot.utils.QueryUtils.queryJoinedEntities
import org.cascadebot.bot.utils.QueryUtils.queryWhereInEntity

class SlotProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        shard: JDA
    ): RabbitMQResponse<*>? {
        if (parts.size != 1) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        val guildId = body.get("guild_id").asLong()
        val guild = shard.getGuildById(guildId)!! // Shard Consumer runs checks, so should not be null

        when (parts[0]) {
            "getAll" -> {
                val list: List<SlotEntry> = Main.postgresManager.transaction {
                    val customCommands = queryJoinedEntities(CustomCommandEntity::class.java, GuildSlotEntity::class.java) { _, join ->
                        equal(join.get<Long>("guildId"), guildId)
                    }.list()
                    val autoResponders = queryJoinedEntities(AutoResponderEntity::class.java, GuildSlotEntity::class.java) { _, join ->
                        equal(join.get<Long>("guildId"), guildId)
                    }.list()

                    customCommands + autoResponders
                }

                return RabbitMQResponse.success(list)
            }
        }

        /*
        *
        */

        TODO("Not yet implemented")
    }
}