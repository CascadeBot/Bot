package org.cascadebot.bot.cmd

import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.cascadebot.bot.Main
import org.cascadebot.bot.MessageType
import org.cascadebot.bot.cmd.meta.CommandArgs
import org.cascadebot.bot.cmd.meta.CommandContext
import org.cascadebot.bot.cmd.meta.RootCommand
import org.cascadebot.bot.db.entities.CommandOptionEntity
import org.cascadebot.bot.db.entities.CustomCommandEntity
import org.cascadebot.bot.db.entities.GuildSlotEntity
import org.cascadebot.bot.utils.QueryUtils.queryEntity
import org.cascadebot.bot.utils.QueryUtils.queryJoinedEntities
import org.cascadebot.bot.utils.tryOrNull
import java.util.UUID

class HelpCommand : RootCommand("help", "Give you a list of all commands available to be run") {
    override val commandData: CommandData = Commands.slash(name, description)
        .addOptions(OptionData(OptionType.STRING, "command", "Command to get help on", false))

    override fun onCommand(context: CommandContext, args: CommandArgs) {
        if (args.getAmount() == 0) {
            handleNoArgs(context)
        } else {
            handleCommandHelp(context, args.getArgAsString("command")!!)
        }
    }

    private fun handleNoArgs(context: CommandContext) {
        // TODO spilt into 3 pages, root commands, custom commands, and modules, for now their all going to be on one page
        val globalBuilder = StringBuilder()
        context.jda.retrieveCommands().queue { commands ->
            for (command in commands) {
                val cascadeCommand = Main.commandManager.getCommand(command.name)
                val slashCommandData = cascadeCommand?.commandData as SlashCommandData
                globalBuilder.append("* </${command.name}:${command.id}> ")
                for (option in slashCommandData.options) {
                    if (option.isRequired) {
                        globalBuilder.append("<${option.name}> ")
                    } else {
                        globalBuilder.append("[${option.name}] ")
                    }
                }
                globalBuilder.append('\n')
            }
            val guildBuilder = StringBuilder()
            context.guild.retrieveCommands().queue { guildCommands ->
                val customCommands = Main.postgres.transaction {
                    queryJoinedEntities(CustomCommandEntity::class.java, GuildSlotEntity::class.java) { _, join ->
                        equal(join.get<Long>("guildId"), context.guild.idLong)
                    }.list();
                }

                for (guildCommand in guildCommands) {
                    val customCommand = customCommands.find { entity -> entity.name == guildCommand.name }

                    if (customCommand == null) {
                        // TODO remove from discord as it does not exist in db?
                        continue
                    }

                    if (doesNotHaveSubCommands(customCommand)) {
                        guildBuilder.append("* </${guildCommand.name}:${guildCommand.id}> ")

                        handleOptions(guildBuilder, customCommand.options)
                        guildBuilder.append("\n")
                    } else {
                        for (option in customCommand.options) {
                            if (option.optionType == org.cascadebot.bot.OptionType.SUB_COMMAND) {
                                guildBuilder.append("* </${guildCommand.name} ${option.name}:${guildCommand.id}> ")

                                handleOptions(guildBuilder, option.subOptions)
                                guildBuilder.append("\n")
                            } else {
                                for (subCommand in option.subOptions) {
                                    guildBuilder.append("* </${guildCommand.name} ${option.name} ${subCommand.name}:${guildCommand.id}> ")

                                    handleOptions(guildBuilder, subCommand.subOptions)
                                    guildBuilder.append("\n")
                                }
                            }
                        }
                    }
                }
                context.replyInfo {
                    field {
                        name = "Core commands"
                        value = globalBuilder.toString()
                        inline = false
                    }
                    field {
                        name = "Custom Commands"
                        value = guildBuilder.toString()
                        inline = false
                    }
                    footer {
                        name = "Run \"/help <command>\" to see more details on a specific command"
                    }
                }
            }
        }
    }

    private fun handleCommandHelp(context: CommandContext, commandStr: String) {
        if (commandStr == "help") {
            easterEggHelp(context)
            return
        }

        context.jda.retrieveCommands().queue { commands ->
            for (command in commands) {
                if (command.name == commandStr) {
                    handleHelpCore(context, command)
                    return@queue
                }
            }

            context.guild.retrieveCommands().queue { guildCommands ->
                for (slot in Main.postgres.transaction {
                    queryEntity(GuildSlotEntity::class.java) { root ->
                        equal(root.get<Long>("guildId"), context.guild.idLong)
                    }.list()
                }) {
                    val split = commandStr.split(" ")

                    for (guildCommand in guildCommands) {
                        if (split[0] != guildCommand.name) {
                            continue
                        }

                        val customCommand = Main.postgres.transaction {
                            return@transaction tryOrNull {
                                queryEntity(CustomCommandEntity::class.java) { root ->
                                    and(
                                        equal(root.get<UUID>("slotId"), slot.slotId),
                                        equal(root.get<String>("name"), guildCommand.name)
                                    )
                                }.singleResult
                            }
                        }
                        if (customCommand == null) {
                            // TODO remove from discord as it does not exist in db?
                            continue
                        }

                        handleHelpCustom(context, split, customCommand, guildCommand.idLong)
                        return@queue
                    }

                    context.reply("Failed to find specified command", MessageType.DANGER)
                }
            }
        }
    }

    private fun handleHelpCore(context: CommandContext, command: Command) {
        val optionsBuilder = StringBuilder()

        val cascadeCommand = Main.commandManager.getCommand(command.name)
        val slashCommandData = cascadeCommand?.commandData as SlashCommandData

        for (option in slashCommandData.options) {
            if (option.isRequired) {
                optionsBuilder.append("* `${option.name}:${option.type.name.lowercase()}` - ${option.description} **(required)**\n")
            } else {
                optionsBuilder.append("* `${option.name}:${option.type.name.lowercase()}` - ${option.description}\n")
            }
        }

        context.replyInfo {
            title = "</${command.name}:${command.id}>"
            description = command.description
            field {
                name = "Options"
                value = optionsBuilder.toString()
                inline = false
            }
        }
    }

    private fun handleHelpCustom(context: CommandContext, path: List<String>, command: CustomCommandEntity, id: Long) {
        if (doesNotHaveSubCommands(command)) {
            if (path.size != 1) {
                subCommandNotFound(context, path, command)
                return
            }

            commandReply(context, command.name, command, command.options, id)
            return
        }

        if (path.size > 3) {
            subCommandNotFound(context, path, command)
            return
        }

        if (path.size == 1) {
            val subCommandsBuilder = StringBuilder()
            val groupsBuilder = StringBuilder()

            for (option in command.options) {
                if (option.optionType == org.cascadebot.bot.OptionType.SUB_COMMAND) {
                    subCommandsBuilder.append("* </${command.name} ${option.name}:${id}> ")
                    handleOptions(subCommandsBuilder, option.subOptions)
                    subCommandsBuilder.append("\n")
                } else {
                    groupsBuilder.append("* `/${command.name} ${option.name}`")
                }
            }

            context.replyInfo {
                title = "</${command.name}:${id}>"
                description = command.description
                field {
                    name = "Sub Commands"
                    value = subCommandsBuilder.toString()
                    inline = false
                }
                field {
                    name = "Sub Command Groups"
                    value = groupsBuilder.toString()
                    inline = false
                }
            }

            return
        }

        for (option in command.options) {
            if (option.name != path[1]) {
                continue
            }

            if (path.size == 2) {
                if (option.optionType == org.cascadebot.bot.OptionType.SUB_COMMAND) {
                    commandReply(context, path.joinToString(" "), command, option.subOptions, id)
                    return
                }

                subCommandNotFound(context, path, command)
                return
            }

            if (option.optionType != org.cascadebot.bot.OptionType.SUBCOMMAND_GROUP) {
                subCommandNotFound(context, path, command)
                return
            }

            for (subCommand in option.subOptions) {
                if (option.name != path[2]) {
                    continue
                }

                commandReply(context, path.joinToString(" "), command, subCommand.subOptions, id)
                return
            }

            subCommandNotFound(context, path, command)
        }
    }

    private fun commandReply(
        context: CommandContext,
        path: String,
        rootCommand: CustomCommandEntity,
        options: List<CommandOptionEntity>,
        id: Long
    ) {
        val optionsBuilder = StringBuilder()
        for (subOption in options) {
            if (subOption.required == true) {
                optionsBuilder.append("* `${subOption.name}:${subOption.optionType.name.lowercase()}` - ${subOption.description} **(required)**\n")
            } else {
                optionsBuilder.append("* `${subOption.name}:${subOption.optionType.name.lowercase()}` - ${subOption.description}\n")
            }
        }

        context.replyInfo {
            title = "</${path}:${id}>"
            description = rootCommand.description
            field {
                name = "Options"
                value = optionsBuilder.toString()
                inline = false
            }
        }
    }

    private fun subCommandNotFound(context: CommandContext, path: List<String>, command: CustomCommandEntity) {
        if (doesNotHaveSubCommands(command)) {
            context.reply("This command does not have any sub commands", MessageType.DANGER)
            return
        }

        context.reply(
            "Was unable to find sub command or sub command group at location " + path.joinToString(" "),
            MessageType.DANGER
        )
    }

    private fun easterEggHelp(context: CommandContext) {
        // TODO better easter egg
        context.reply("Did you just run help on the help command?")
    }

    private fun handleOptions(builder: StringBuilder, options: List<CommandOptionEntity>) {
        for (option in options) {
            if (option.required == true) {
                builder.append("<${option.name}> ")
            } else {
                builder.append("[${option.name}] ")
            }
        }
    }

    private fun doesNotHaveSubCommands(command: CustomCommandEntity): Boolean {
        if (command.options.size == 0) {
            return true
        }
        return command.options[0].optionType == org.cascadebot.bot.OptionType.SUB_COMMAND || command.options[0].optionType == org.cascadebot.bot.OptionType.SUBCOMMAND_GROUP
    }
}