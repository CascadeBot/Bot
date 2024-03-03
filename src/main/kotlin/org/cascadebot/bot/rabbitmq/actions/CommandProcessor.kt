package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import net.dv8tion.jda.api.entities.Guild
import org.cascadebot.bot.Main
import org.cascadebot.bot.SlotType
import org.cascadebot.bot.db.entities.CustomCommandEntity
import org.cascadebot.bot.db.entities.GuildSlotEntity
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.CreateCustomCommandRequest
import org.cascadebot.bot.rabbitmq.objects.CustomCommandResponse
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.UpdateCustomCommandRequest

class CommandProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        context: RabbitMQContext,
        guild: Guild
    ): RabbitMQResponse<*>? {
        /*
        * Update
        *
        */

        if (parts.isEmpty()) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        return when {
            checkAction(parts, "create") -> createCustomCommand(body, guild)
            checkAction(parts, "update") -> updateCustomCommand(body, guild, context)

            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun createCustomCommand(
        body: ObjectNode,
        guild: Guild
    ): RabbitMQResponse<CustomCommandResponse> {
        val createRequest = Main.json.treeToValue<CreateCustomCommandRequest>(body)

        val slot = GuildSlotEntity(SlotType.CUSTOM_CMD, guild.idLong)
        val customCommand =
            CustomCommandEntity(
                slot.slotId,
                createRequest.name,
                createRequest.description,
                createRequest.marketplaceRef,
                createRequest.type,
                createRequest.lang,
                createRequest.ephemeral
            )

        dbTransaction {
            persist(slot)
            persist(customCommand)
        }

        // We've just created it, the command won't be enabled yet
        return RabbitMQResponse.success(CustomCommandResponse.fromEntity(false, customCommand))
    }

    private fun updateCustomCommand(
        body: ObjectNode,
        guild: Guild,
        context: RabbitMQContext
    ): RabbitMQResponse<CustomCommandResponse>? {
        val updateRequest = Main.json.treeToValue<UpdateCustomCommandRequest>(body)

        val command: CustomCommandEntity? = dbTransaction {
            get(CustomCommandEntity::class.java, updateRequest.slotId)
        }

        if (command == null || command.slot.guildId != guild.idLong) return CommonResponses.CUSTOM_COMMAND_NOT_FOUND

        command.apply {
            name = updateRequest.name
            description = updateRequest.description
            marketplaceRef = updateRequest.marketplaceRef
            type = updateRequest.type
            lang = updateRequest.lang
            entrypoint = updateRequest.entrypoint
            ephemeral = updateRequest.ephemeral
        }

        dbTransaction {
            persist(command)
        }

        guild.retrieveCommands().queue({ commands ->
            val exists =
                commands.any { it.applicationIdLong == Main.applicationInfo.idLong && it.name == command.name }

            RabbitMQResponse.success(CustomCommandResponse.fromEntity(exists, command))
                .sendAndAck(context)
        }, {
            RabbitMQResponse.success(CustomCommandResponse.fromEntity(false, command))
                .sendAndAck(context)
        })

        return null;
    }


}