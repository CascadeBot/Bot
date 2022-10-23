package org.cascadebot.bot.events

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.cascadebot.bot.Main
import org.cascadebot.bot.MessageType
import org.cascadebot.bot.ScriptLang
import org.cascadebot.bot.cmd.meta.CommandArgs
import org.cascadebot.bot.cmd.meta.CommandContext
import org.cascadebot.bot.db.entities.CustomCommandEntity
import org.cascadebot.bot.db.entities.GuildSlotEntity
import org.cascadebot.bot.db.entities.ScriptFileEntity
import org.cascadebot.bot.rabbitmq.objects.ChannelResponse
import org.cascadebot.bot.rabbitmq.objects.CommandOption
import org.cascadebot.bot.rabbitmq.objects.ExecuteCommandReq
import org.cascadebot.bot.rabbitmq.objects.MemberResponse
import org.cascadebot.bot.rabbitmq.objects.RabbitMqMessage
import org.cascadebot.bot.rabbitmq.objects.RoleResponse
import org.cascadebot.bot.rabbitmq.objects.ScriptFile
import java.util.UUID

class InteractionListener : ListenerAdapter() {

    private val logger by SLF4J

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!event.isFromGuild) {
            return
        }

        val context = CommandContext(event)
        val args = CommandArgs(event.options.groupBy { it.name })

        if (event.isGlobalCommand) {
            val command = Main.commandManager.getCommand(event.commandPath)
            if (command == null) {
                context.reply("Command not recognised!", MessageType.DANGER)
                return
            }
            logger.info(
                "Command {} executed by {} with args: {}",
                event.commandPath,
                context.user.asTag,
                event.options.joinToString(", ", "[", "]") { it.name }
            )
            // TODO add exception handling
            command.onCommand(context, args)
            return
        }

        val name = event.name
        val command = Main.postgresManager.transaction {
            val builder = criteriaBuilder
            val query = builder.createQuery(CustomCommandEntity::class.java)
            val root = query.from(CustomCommandEntity::class.java)
            val join = root.join(GuildSlotEntity::class.java)
            query.where(
                builder.and(
                    builder.equal(join.get<Long>("guildId"), event.guild!!.idLong),
                    builder.equal(root.get<String>("name"), name),
                    builder.or(
                        builder.equal(join.get<Boolean>("enabled"), true),
                        builder.isNull(join.get<Boolean>("enabled"))
                    )
                )
            )
            createQuery(query).singleResult
        }

        if (command != null) {
            loadAndExecuteCustomCommand(event, command)
            return
        }

        // TODO command not found

    }

    private fun loadAndExecuteCustomCommand(event: SlashCommandInteractionEvent, command: CustomCommandEntity) {
        val files = Main.postgresManager.transaction {
            val builder = criteriaBuilder
            val query = builder.createQuery(ScriptFileEntity::class.java)
            val root = query.from(ScriptFileEntity::class.java)
            query.where(builder.equal(root.get<UUID>("slotId"), command.slotId))
            return@transaction createQuery(query).list()
        }!!

        val info = if (command.entrypoint != null) {
            EntrypointInfo(command.entrypoint!!, command.ephemeral!!)
        } else if (event.subcommandGroup != null) {
            val groupOpt = command.options.first { it.name == event.subcommandGroup }
            val subOpt = groupOpt.subOptions.first { it.name == event.subcommandName }
            EntrypointInfo(subOpt.entrypoint!!, subOpt.ephemeral!!)
        } else {
            val subOpt = command.options.first { it.name == event.subcommandName }
            EntrypointInfo(subOpt.entrypoint!!, subOpt.ephemeral!!)
        }

        event.deferReply(info.ephemeral).queue { hook ->
            hook.retrieveOriginal().queue {
                val options: MutableMap<String, MutableList<CommandOption<Any>>> = mutableMapOf()
                for (discOpt in event.options) {
                    val list = if (options.containsKey(discOpt.name)) {
                        options[discOpt.name]
                    } else {
                        val newList: MutableList<CommandOption<Any>> = mutableListOf()
                        options[discOpt.name] = newList
                        newList
                    }!!
                    when (discOpt.type) {
                        OptionType.STRING -> {
                            list.add(CommandOption(org.cascadebot.bot.OptionType.STRING, discOpt.asString))
                        }

                        OptionType.INTEGER -> {
                            list.add(CommandOption(org.cascadebot.bot.OptionType.NUMBER, discOpt.asInt))
                        }

                        OptionType.BOOLEAN -> {
                            list.add(CommandOption(org.cascadebot.bot.OptionType.BOOLEAN, discOpt.asBoolean))
                        }

                        OptionType.USER -> {
                            list.add(
                                CommandOption(
                                    org.cascadebot.bot.OptionType.USER,
                                    MemberResponse.fromMember(discOpt.asMember!!)
                                )
                            )
                        }

                        OptionType.CHANNEL -> {
                            list.add(
                                CommandOption(
                                    org.cascadebot.bot.OptionType.CHANNEL,
                                    ChannelResponse.fromChannel(discOpt.asChannel.asStandardGuildChannel())
                                )
                            )
                        }

                        OptionType.ROLE -> {
                            list.add(
                                CommandOption(
                                    org.cascadebot.bot.OptionType.ROLE,
                                    RoleResponse.fromRole(discOpt.asRole)
                                )
                            )
                        }

                        OptionType.NUMBER -> {
                            list.add(CommandOption(org.cascadebot.bot.OptionType.NUMBER, discOpt.asDouble))
                        }

                        else -> {}
                    }
                }

                if (command.lang == ScriptLang.TEXT) {
                    // TODO bot processes text commands
                    return@queue
                }

                val scriptFiles: MutableList<ScriptFile> = mutableListOf()
                for (file in files) {
                    scriptFiles.add(ScriptFile(file.scriptId, file.fileName, file.script))
                }

                val req = ExecuteCommandReq(
                    command.lang,
                    info.entrypoint,
                    scriptFiles,
                    options,
                    MemberResponse.fromMember(event.member!!),
                    ChannelResponse.fromChannel(event.channel.asTextChannel()),
                    RabbitMqMessage.fromDiscordMessage(it)
                )
                // TODO send req via rabbitmq
            }
        }

    }

    data class EntrypointInfo(val entrypoint: UUID, val ephemeral: Boolean)

}