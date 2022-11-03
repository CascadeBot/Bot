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
import org.cascadebot.bot.rabbitmq.objects.AutoResponderResponse
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.CustomCommandResponse
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQException
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.utils.QueryUtils.queryEntity
import org.cascadebot.bot.utils.QueryUtils.queryJoinedEntities
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
                val (slots, commands, responders) = Main.postgresManager.transaction {
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
                val commandsResponse = commands.map { CustomCommandResponse.fromEntity(slotsMap[it.slotId]!!, it) }
                val autoResponse = responders.map { AutoResponderResponse.fromEntity(slotsMap[it.slotId]!!, it) }

                return RabbitMQResponse.success(commandsResponse + autoResponse)
            }

            "get" -> {
                val slot = getSlot(body, guildId)

                val (command, responder) = Main.postgresManager.transaction {
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
                        val command = Main.postgresManager.transaction {
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

                        guild.retrieveCommands().queue { commands ->
                            val exists =
                                commands.any { it.applicationIdLong == Main.applicationInfo.idLong && it.name == command.name }

                            RabbitMQResponse.success("enabled", exists)
                        }
                    }

                    slot.isAutoResponder -> {
                        return RabbitMQResponse.success("enabled", slot.enabled)
                    }

                    else -> {
                        return RabbitMQResponse.failure(
                            StatusCode.ServerException,
                            MiscErrorCodes.UnexpectedError,
                            "Slot is an unsupported type"
                        )
                    }
                }

            }

            "enable" -> {

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

        val slot = Main.postgresManager.transaction {
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

}