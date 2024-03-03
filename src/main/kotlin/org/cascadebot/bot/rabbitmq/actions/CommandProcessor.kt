package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.entities.Guild
import org.cascadebot.bot.Main
import org.cascadebot.bot.SlotType
import org.cascadebot.bot.db.entities.CustomCommandEntity
import org.cascadebot.bot.db.entities.GuildSlotEntity
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.CreateCustomCommandRequest
import org.cascadebot.bot.rabbitmq.objects.CustomCommandResponse
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse

class CommandProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        guild: Guild
    ): RabbitMQResponse<*> {
        /*
        * Update
        *
        */

        if (parts.isEmpty()) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        return when {
            checkAction(parts, "create") -> createCustomCommand(body, guild)

            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun createCustomCommand(
        body: ObjectNode,
        guild: Guild
    ): RabbitMQResponse<CustomCommandResponse> {
        val createRequest = Main.json.treeToValue(body, CreateCustomCommandRequest::class.java)

        val slot = GuildSlotEntity(SlotType.CUSTOM_CMD, guild.idLong)
        val customCommand =
            CustomCommandEntity(
                slot.slotId,
                createRequest.name,
                createRequest.description,
                createRequest.type,
                createRequest.lang,
                createRequest.ephemeral
            )

        dbTransaction {
            persist(slot)
            persist(customCommand)
        }

        return RabbitMQResponse.success(CustomCommandResponse.fromEntity(false, customCommand))
    }


}