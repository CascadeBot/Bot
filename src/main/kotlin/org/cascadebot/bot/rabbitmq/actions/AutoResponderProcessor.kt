package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import net.dv8tion.jda.api.entities.Guild
import org.cascadebot.bot.Main
import org.cascadebot.bot.SlotType
import org.cascadebot.bot.db.entities.AutoResponderEntity
import org.cascadebot.bot.db.entities.GuildSlotEntity
import org.cascadebot.bot.rabbitmq.objects.AutoResponderResponse
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.CreateAutoResponderRequest
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse

class AutoResponderProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        context: RabbitMQContext,
        guild: Guild
    ): RabbitMQResponse<*> {
        if (parts.isEmpty()) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        return when {
            checkAction(parts, "create") -> createAutoResponder(body, guild)

            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun createAutoResponder(
        body: ObjectNode,
        guild: Guild
    ): RabbitMQResponse<AutoResponderResponse> {
        val createRequest = Main.json.treeToValue<CreateAutoResponderRequest>(body)

        val slot = GuildSlotEntity(SlotType.AUTO_REPLY, guild.idLong)
        val autoResponder =
            AutoResponderEntity(
                slot.slotId,
                createRequest.text,
                createRequest.matchText.toMutableList(),
                createRequest.enabled
            )

        dbTransaction {
            persist(slot)
            persist(autoResponder)
        }

        return RabbitMQResponse.success(
            AutoResponderResponse.fromEntity(autoResponder)
        )
    }
}