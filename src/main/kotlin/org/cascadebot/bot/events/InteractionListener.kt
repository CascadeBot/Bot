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
import org.cascadebot.bot.rabbitmq.objects.ExecCommandRequest
import org.cascadebot.bot.rabbitmq.objects.MemberResponse
import org.cascadebot.bot.rabbitmq.objects.MessageResponse
import org.cascadebot.bot.rabbitmq.objects.RoleResponse
import org.cascadebot.bot.rabbitmq.objects.ScriptFileData
import org.cascadebot.bot.rabbitmq.objects.UserResponse
import java.util.UUID
import org.cascadebot.bot.OptionType as OptType

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
        }

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
                    val list = options.getOrPut(discOpt.name) { mutableListOf() }

                    val commandOption: CommandOption<Any>? = when (discOpt.type) {
                        OptionType.STRING -> CommandOption(OptType.STRING, discOpt.asString)
                        OptionType.INTEGER -> CommandOption(OptType.NUMBER, discOpt.asInt)
                        OptionType.BOOLEAN -> CommandOption(OptType.BOOLEAN, discOpt.asBoolean)

                        OptionType.USER -> {
                            val member = discOpt.asMember
                            if (member != null) {
                                CommandOption(OptType.MEMBER, MemberResponse.fromMember(member))
                            } else {
                                CommandOption(OptType.USER, UserResponse.fromUser(discOpt.asUser))
                            }
                        }

                        OptionType.CHANNEL -> {
                            CommandOption(
                                OptType.CHANNEL,
                                ChannelResponse.fromChannel(discOpt.asChannel.asStandardGuildChannel())
                            )
                        }

                        OptionType.ROLE -> CommandOption(OptType.ROLE, RoleResponse.fromRole(discOpt.asRole))
                        OptionType.NUMBER -> CommandOption(OptType.NUMBER, discOpt.asDouble)
                        else -> null
                    }

                    if (commandOption != null) {
                        list.add(commandOption)
                    }
                }

                if (command.lang == ScriptLang.TEXT) {
                    // TODO bot processes text commands
                    return@queue
                }

                val scriptFiles: MutableList<ScriptFileData> = mutableListOf()
                for (file in files) {
                    scriptFiles.add(ScriptFileData(file.scriptId, file.fileName, file.script))
                }

                val req = ExecCommandRequest(
                    command.lang,
                    info.entrypoint,
                    scriptFiles,
                    options,
                    MemberResponse.fromMember(event.member!!),
                    ChannelResponse.fromChannel(event.channel.asTextChannel()),
                    MessageResponse.fromDiscordMessage(it)
                )
                // TODO send req via rabbitmq
            }
        }

    }

    data class EntrypointInfo(val entrypoint: UUID, val ephemeral: Boolean)

}