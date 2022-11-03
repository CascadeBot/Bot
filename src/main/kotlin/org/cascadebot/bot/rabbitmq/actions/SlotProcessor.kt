package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import dev.minn.jda.ktx.interactions.commands.Subcommand
import dev.minn.jda.ktx.interactions.commands.SubcommandGroup
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.upsertCommand
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.cascadebot.bot.CustomCommandType
import org.cascadebot.bot.Main
import org.cascadebot.bot.OptionType
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

                        val commandData = when (command.type) {
                            CustomCommandType.SLASH -> {
                                val data = Commands.slash(command.name, command.description ?: "No description")
                                data.isGuildOnly = true
                                command.options.forEach { option ->
                                    when (option.optionType) {
                                        OptionType.SUB_COMMAND -> {
                                            data.addSubcommands(Subcommand(option.name, option.description) {
                                                option.subOptions.forEach { subOption ->
                                                    data.addOption(subOption.optionType.jdaOptionType, subOption.name, subOption.description, subOption.required ?: false, subOption.autocomplete ?: false)
                                                }
                                            })
                                        }
                                        OptionType.SUBCOMMAND_GROUP -> {
                                            data.addSubcommandGroups(SubcommandGroup(option.name, option.description) {
                                                option.subOptions.forEach { subCommand ->
                                                    data.addOption(subCommand.optionType.jdaOptionType, subCommand.name, subCommand.description, subCommand.required ?: false, subCommand.autocomplete ?: false)
                                                    subCommand.subOptions.forEach { subOption ->
                                                        data.addOption(subOption.optionType.jdaOptionType, subOption.name, subOption.description, subOption.required ?: false, subOption.autocomplete ?: false)
                                                    }
                                                }
                                            })
                                        }
                                        else -> {
                                            data.addOption(option.optionType.jdaOptionType, option.name, option.description, option.required ?: false, option.autocomplete ?: false)
                                        }
                                    }
                                }
                                data
                            }
                            CustomCommandType.CONTEXT_USER -> {
                                val data = Commands.user(command.name)
                                data.isGuildOnly = true
                                data
                            }
                            CustomCommandType.CONTEXT_MESSAGE -> {
                                val data = Commands.message(command.name)
                                data.isGuildOnly = true
                                data
                            }
                        }

                        guild.upsertCommand(commandData).queue {

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