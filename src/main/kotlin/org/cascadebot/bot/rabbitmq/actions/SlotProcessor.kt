package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
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
        shard: JDA
    ): RabbitMQResponse<*>? {
        if (parts.size != 1) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        val guildId = body.get("guild_id").asLong()
        val guild = shard.getGuildById(guildId)!! // Shard Consumer runs checks, so should not be null

        when (parts[0]) {
            "getAll" -> {
                val (slots, commands, responders) = dbTransaction {
                    val guildSlots = queryEntity(GuildSlotEntity::class.java) { root ->
                        equal(root.get<Long>("guildId"), guildId)
                    }.list()
                    val customCommands =
                        queryJoinedEntities(CustomCommandEntity::class.java, GuildSlotEntity::class.java) { _, join ->
                            equal(join.get<Long>("guildId"), guildId)
                        }.list()
                    val autoResponders =
                        queryJoinedEntities(AutoResponderEntity::class.java, GuildSlotEntity::class.java) { _, join ->
                            equal(join.get<Long>("guildId"), guildId)
                        }.list()

                    Triple(guildSlots, customCommands, autoResponders)
                }

                val slotsMap = slots.associateBy { it.slotId }

                // !! is okay here because the existence of the slot ID is enforced at DB level for each slot item
                val commandsResponse = commands.map { CustomCommandResponse.fromEntity(slotsMap[it.slotId]!!.enabled ?: false, it) }
                val autoResponse = responders.map { AutoResponderResponse.fromEntity(slotsMap[it.slotId]!!, it) }

                return RabbitMQResponse.success(commandsResponse + autoResponse)
            }

            "get" -> {
                val slot = getSlot(body, guildId)

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
                            return RabbitMQResponse.failure(
                                StatusCode.NotFound,
                                MiscErrorCodes.SlotNotFound,
                                "A custom command for the slot specified could not be found"
                            )
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
                        return RabbitMQResponse.success(AutoResponderResponse.fromEntity(slot, responder))
                    }

                    else -> RabbitMQResponse.failure(
                        StatusCode.ServerException,
                        MiscErrorCodes.UnexpectedError,
                        "Slot is an unsupported type"
                    )
                }
            }

            "isEnabled" -> {
                val slot = getSlot(body, guildId)

                when {
                    slot.isCustomCommand -> {
                        val command = dbTransaction {
                            tryOrNull {
                                queryEntity(CustomCommandEntity::class.java) { root ->
                                    equal(root.get<UUID>("slotId"), slot.slotId)
                                }.singleResult
                            }
                        }

                        if (command == null) {
                            return RabbitMQResponse.failure(
                                StatusCode.NotFound,
                                MiscErrorCodes.SlotNotFound,
                                "A custom command for the slot specified could not be found"
                            )
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
                        val response = createJsonObject(
                            "slot_id" to slot.slotId,
                            "enabled" to slot.enabled
                        )
                        return RabbitMQResponse.success(response)
                    }

                    else -> return CommonResponses.UNSUPPORTED_SLOT
                }

            }

            "enable" -> {
                val slot = getSlot(body, guildId)

                when {
                    slot.isCustomCommand -> {
                        val command = dbTransaction {
                            tryOrNull {
                                queryEntity(CustomCommandEntity::class.java) { root ->
                                    equal(root.get<UUID>("slotId"), slot.slotId)
                                }.singleResult
                            }
                        }

                        if (command == null) {
                            return RabbitMQResponse.failure(
                                StatusCode.NotFound,
                                MiscErrorCodes.SlotNotFound,
                                "A custom command for the slot specified could not be found"
                            )
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
                        slot.enabled = true

                        dbTransaction {
                            persist(slot)
                        }

                        val response = createJsonObject(
                            "slot_id" to slot.slotId,
                            "enabled" to slot.enabled
                        )

                        return RabbitMQResponse.success(response)
                    }

                    else -> return CommonResponses.UNSUPPORTED_SLOT
                }

            }

            "disable" -> {
                val slot = getSlot(body, guildId)

                when {
                    slot.isCustomCommand -> {
                        val command = dbTransaction {
                            tryOrNull {
                                queryEntity(CustomCommandEntity::class.java) { root ->
                                    equal(root.get<UUID>("slotId"), slot.slotId)
                                }.singleResult
                            }
                        }

                        if (command == null) {
                            return RabbitMQResponse.failure(
                                StatusCode.NotFound,
                                MiscErrorCodes.SlotNotFound,
                                "A custom command for the slot specified could not be found"
                            )
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
                        slot.enabled = false

                        dbTransaction {
                            persist(slot)
                        }

                        val response = createJsonObject(
                            "slot_id" to slot.slotId,
                            "enabled" to slot.enabled
                        )

                        return RabbitMQResponse.success(response)
                    }
                }

            }


        }

        /*
        *
        */

        return CommonResponses.UNSUPPORTED_ACTION
    }

    private fun getSlot(
        body: ObjectNode,
        guildId: Long
    ): GuildSlotEntity {
        val slotId = getSlotId(body)
        
        val slot = dbTransaction {
            tryOrNull {
                queryEntity(GuildSlotEntity::class.java) { root ->
                    equal(root.get<Long>("guildId"), guildId)
                    equal(root.get<UUID>("slotId"), slotId)
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