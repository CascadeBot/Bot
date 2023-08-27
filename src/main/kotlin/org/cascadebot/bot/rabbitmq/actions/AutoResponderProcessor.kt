package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.entities.Guild
import org.cascadebot.bot.Main
import org.cascadebot.bot.SlotType
import org.cascadebot.bot.db.entities.AutoResponderEntity
import org.cascadebot.bot.db.entities.GuildSlotEntity
import org.cascadebot.bot.rabbitmq.objects.AutoResponderResponse
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.CreateAutoResponderRequest
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse

class AutoResponderProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        guild: Guild
    ): RabbitMQResponse<*>? {
        if (parts.isEmpty()) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        when (parts[0]) {
            "create" -> {
                val createRequest = Main.json.treeToValue(body, CreateAutoResponderRequest::class.java)

                val slot = GuildSlotEntity(SlotType.AUTO_REPLY, guild.idLong)
                slot.enabled = createRequest.enabled
                val autoResponder =
                    AutoResponderEntity(slot.slotId, createRequest.text, createRequest.matchText.toMutableList())

                dbTransaction {
                    persist(slot)
                    persist(autoResponder)
                }

                return RabbitMQResponse.success(
                    AutoResponderResponse(
                        slot.slotId,
                        slot.enabled,
                        autoResponder.text,
                        autoResponder.match
                    )
                )
            }
        }

        return CommonResponses.UNSUPPORTED_ACTION
    }
}