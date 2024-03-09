package org.cascadebot.bot.cmd

import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.cascadebot.bot.Main
import org.cascadebot.bot.cmd.meta.CommandArgs
import org.cascadebot.bot.cmd.meta.CommandContext
import org.cascadebot.bot.cmd.meta.RootCommand
import org.cascadebot.bot.db.entities.CustomCommandEntity
import org.cascadebot.bot.db.entities.GuildSlotEntity
import org.cascadebot.bot.utils.QueryUtils.queryEntity
import org.cascadebot.bot.utils.tryOrNull
import java.util.UUID

class HelpCommand : RootCommand("help", "Give you a list of all commands available to be run") {
    override val commandData: CommandData = Commands.slash(name, description)
        .addOptions(OptionData(OptionType.STRING, "command", "Command to get help on", false))

    override fun onCommand(context: CommandContext, args: CommandArgs) {
        if (args.getAmount() == 0) {
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
                    for (slot in Main.postgres.transaction {
                        queryEntity(GuildSlotEntity::class.java) { root ->
                            equal(root.get<Long>("guildId"), context.guild.idLong)
                        }.list()
                    }) {
                        for (guildCommand in guildCommands) {
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
                            if (doesNotHaveSubCommands(customCommand)) {
                                guildBuilder.append("* </${guildCommand.name}:${guildCommand.id}> ")

                                for (option in customCommand.options) {
                                    if (option.required == true) {
                                        guildBuilder.append("<${option.name}> ")
                                    } else {
                                        guildBuilder.append("[${option.name}] ")
                                    }
                                }
                                guildBuilder.append("\n")
                            } else {
                                for (option in customCommand.options) {
                                    if (option.optionType == org.cascadebot.bot.OptionType.SUB_COMMAND) {
                                        guildBuilder.append("* </${guildCommand.name} ${option.name}:${guildCommand.id}> ")

                                        for (subOption in option.subOptions) {
                                            if (subOption.required == true) {
                                                guildBuilder.append("<${subOption.name}> ")
                                            } else {
                                                guildBuilder.append("[${subOption.name}] ")
                                            }
                                        }
                                    } else {
                                        for (subCommand in option.subOptions) {
                                            guildBuilder.append("* </${guildCommand.name} ${option.name} ${subCommand.name}:${guildCommand.id}> ")

                                            for (subOption in subCommand.subOptions) {
                                                if (subOption.required == true) {
                                                    guildBuilder.append("<${subOption.name}> ")
                                                } else {
                                                    guildBuilder.append("[${subOption.name}] ")
                                                }
                                            }
                                            guildBuilder.append("\n")
                                        }
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
                    }
                }
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