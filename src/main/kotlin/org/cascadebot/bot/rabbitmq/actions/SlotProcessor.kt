package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import org.cascadebot.bot.Main
import org.cascadebot.bot.db.entities.AutoResponderEntity
import org.cascadebot.bot.db.entities.CustomCommandEntity
import org.cascadebot.bot.db.entities.GuildSlotEntity
import org.cascadebot.bot.rabbitmq.objects.AutoResponderResponse
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.CustomCommandResponse
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQException
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.QueryUtils.deleteEntity
import org.cascadebot.bot.utils.QueryUtils.queryEntity
import org.cascadebot.bot.utils.QueryUtils.queryJoinedEntities
import org.cascadebot.bot.utils.createJsonObject
import org.cascadebot.bot.utils.tryOrNull
import java.util.UUID

class SlotProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        guild: Guild
    ): RabbitMQResponse<*>? {
        if (parts.size != 1) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        when (parts[0]) {
            "getAll" -> {
                val (commands, responders) = dbTransaction {
                    val customCommands =
                        queryJoinedEntities(CustomCommandEntity::class.java, GuildSlotEntity::class.java) { _, join ->
                            equal(join.get<Long>("guildId"), guild.idLong)
                        }.list()
                    val autoResponders =
                        queryJoinedEntities(AutoResponderEntity::class.java, GuildSlotEntity::class.java) { _, join ->
                            equal(join.get<Long>("guildId"), guild.idLong)
                        }.list()

                    Pair(customCommands, autoResponders)
                }

                // TODO Query Discord
                val commandsResponse = commands.map { CustomCommandResponse.fromEntity(false, it) }
                val autoResponse = responders.map { AutoResponderResponse.fromEntity(it) }

                return RabbitMQResponse.success(commandsResponse + autoResponse)
            }

            "get" -> {
                val slot = getSlot(body, guild.idLong)

                val (command, responder) = dbTransaction {
                    val customCommand = tryOrNull {
                        queryEntity(CustomCommandEntity::class.java) { root ->
                            equal(root.get<UUID>("slotId"), slot.slotId)
                        }.singleResult
                    }
                    val autoResponder = tryOrNull {
                        queryEntity(AutoResponderEntity::class.java) { root ->
                            equal(root.get<UUID>("slotId"), slot.slotId)
                        }.singleResult
                    }

                    Pair(customCommand, autoResponder)
                }

                when {
                    slot.isCustomCommand -> {
                        if (command == null) {
                            return CommonResponses.CUSTOM_COMMAND_NOT_FOUND
                        }

                        guild.retrieveCommands().queue { commands ->
                            val exists =
                                commands.any { it.applicationIdLong == Main.applicationInfo.idLong && it.name == command.name }

                            RabbitMQResponse.success(CustomCommandResponse.fromEntity(exists, command))
                        }

                        return null
                    }

                    slot.isAutoResponder -> {
                        if (responder == null) {
                            return RabbitMQResponse.failure(
                                StatusCode.NotFound,
                                MiscErrorCodes.SlotNotFound,
                                "A auto responder for the slot specified could not be found"
                            )
                        }
                        return RabbitMQResponse.success(AutoResponderResponse.fromEntity(responder))
                    }

                    else -> RabbitMQResponse.failure(
                        StatusCode.ServerException,
                        MiscErrorCodes.UnexpectedError,
                        "Slot is an unsupported type"
                    )
                }
            }

            "isEnabled" -> {
                val slot = getSlot(body, guild.idLong)

                when {
                    slot.isCustomCommand -> {
                        val command = getCommand(slot)

                        if (command == null) {
                            return CommonResponses.CUSTOM_COMMAND_NOT_FOUND
                        }

                        guild.retrieveCommands().queue({ commands ->
                            val exists =
                                commands.any { it.applicationIdLong == Main.applicationInfo.idLong && it.name == command.name }

                            val response = createJsonObject(
                                "slot_id" to slot.slotId,
                                "enabled" to exists
                            )

                            RabbitMQResponse.success(response)
                        }, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))
                        return null
                    }

                    slot.isAutoResponder -> {
                        val autoResponder = getAutoResponder(slot)

                        if (autoResponder == null) {
                            return CommonResponses.AUTORESPONDER_NOT_FOUND
                        }

                        val response = createJsonObject(
                            "slot_id" to slot.slotId,
                            "enabled" to autoResponder.enabled
                        )
                        return RabbitMQResponse.success(response)
                    }

                    else -> return CommonResponses.UNSUPPORTED_SLOT
                }

            }

            "enable" -> {
                val slot = getSlot(body, guild.idLong)

                when {
                    slot.isCustomCommand -> {
                        val command = getCommand(slot)

                        if (command == null) {
                            return CommonResponses.CUSTOM_COMMAND_NOT_FOUND
                        }

                        val commandData = command.toDiscordCommand()

                        // Important! ALl custom commands are guild-only.
                        commandData.isGuildOnly = true

                        guild.upsertCommand(commandData).queue({
                            val obj = createJsonObject(
                                "slot_id" to slot.slotId,
                                "enabled" to true,
                                "command_id" to it.id,
                                "name" to it.name
                            )
                            RabbitMQResponse.success(obj)
                        }, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))
                        return null
                    }

                    slot.isAutoResponder -> {
                        val autoResponder = getAutoResponder(slot)

                        if (autoResponder == null) {
                            return CommonResponses.AUTORESPONDER_NOT_FOUND
                        }

                        autoResponder.enabled = true

                        dbTransaction {
                            persist(autoResponder)
                        }

                        val response = createJsonObject(
                            "slot_id" to slot.slotId,
                            "enabled" to autoResponder.enabled
                        )

                        return RabbitMQResponse.success(response)
                    }

                    else -> return CommonResponses.UNSUPPORTED_SLOT
                }

            }

            "disable" -> {
                val slot = getSlot(body, guild.idLong)

                when {
                    slot.isCustomCommand -> {
                        val command = getCommand(slot)

                        if (command == null) {
                            return CommonResponses.CUSTOM_COMMAND_NOT_FOUND
                        }

                        val deleteCommand: (List<Command>) -> Unit = { commands: List<Command> ->
                            val cmd =
                                commands.find { it.name == command.name && it.applicationIdLong == Main.applicationInfo.idLong }

                            cmd?.delete()?.queue({
                                val response = createJsonObject(
                                    "slot_id" to slot.slotId,
                                    "enabled" to false
                                )
                                RabbitMQResponse.success(response).sendAndAck(rabbitMqChannel, properties, envelope)
                            }, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))
                        }

                        guild.retrieveCommands()
                            .queue(deleteCommand, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))
                        return null
                    }

                    slot.isAutoResponder -> {
                        val autoResponder = getAutoResponder(slot)

                        if (autoResponder == null) {
                            return CommonResponses.AUTORESPONDER_NOT_FOUND
                        }

                        autoResponder.enabled = false

                        dbTransaction {
                            persist(autoResponder)
                        }

                        val response = createJsonObject(
                            "slot_id" to slot.slotId,
                            "enabled" to autoResponder.enabled
                        )

                        return RabbitMQResponse.success(response)
                    }
                }

            }

            "delete" -> {
                val slotId = getSlotId(body)
                
                val numDeleted = dbTransaction {
                    deleteEntity(GuildSlotEntity::class.java) { root ->
                        equal(root.get<UUID>("slotId"), slotId)
                    }
                }

                if (numDeleted == 0) {
                    return RabbitMQResponse.failure(
                        StatusCode.NotFound,
                        MiscErrorCodes.SlotNotFound,
                        "Slot could not be found"
                    )
                }

                return RabbitMQResponse.success(createJsonObject("slot_id" to slotId))
            }

        }

        /*
        *
        */

        return CommonResponses.UNSUPPORTED_ACTION
    }

    private fun getCommand(slot: GuildSlotEntity): CustomCommandEntity? {
        val command = dbTransaction {
            tryOrNull {
                queryEntity(CustomCommandEntity::class.java) { root ->
                    equal(root.get<UUID>("slotId"), slot.slotId)
                }.singleResult
            }
        }

        return command
    }

    private fun getAutoResponder(slot: GuildSlotEntity): AutoResponderEntity? {
        val autoResponder = dbTransaction {
            tryOrNull {
                queryEntity(AutoResponderEntity::class.java) { root ->
                    equal(root.get<UUID>("slotId"), slot.slotId)
                }.singleResult
            }
        }

        return autoResponder
    }

    private fun getSlot(
        body: ObjectNode,
        guildId: Long
    ): GuildSlotEntity {
        val slotId = getSlotId(body)

        val slot = dbTransaction {
            tryOrNull {
                queryEntity(GuildSlotEntity::class.java) { root ->
                    and(
                        equal(root.get<Long>("guildId"), guildId),
                        equal(root.get<UUID>("slotId"), slotId)
                    )
                }.singleResult
            }
        }

        if (slot == null) {
            throw RabbitMQException(
                RabbitMQResponse.failure(
                    StatusCode.NotFound,
                    MiscErrorCodes.SlotNotFound,
                    "Slot could not be found"
                )
            )
        }

        return slot
    }

    private fun getSlotId(body: ObjectNode): UUID {
        // If slot_id is not present, or UUID is invalid, then this will return null (aka invalid)
        val slotId = body.get("slot_id")?.asText()?.let { tryOrNull { UUID.fromString(it) } }

        if (slotId == null) {
            throw RabbitMQException(
                RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidSlot,
                    "Slot ID must be provided and in UUID format"
                )
            )
        }
        return slotId
    }

}