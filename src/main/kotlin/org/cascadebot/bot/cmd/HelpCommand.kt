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

class HelpCommand : RootCommand("help", "Give you a list of all commands available to be run") {
    override val commandData: CommandData = Commands.slash(name, description).addOptions(OptionData(OptionType.STRING, "command", "Command to get help on", false))

    override fun onCommand(context: CommandContext, args: CommandArgs) {
        // TODO spilt into 3 pages, root commands, custom commands, and modules, for now their all going to be on one page
        if (args.getAmount() == 0) {
            val builder = StringBuilder()
            context.jda.retrieveCommands().queue { commands ->
                for (command in commands) {
                    val cascadeCommand = Main.commandManager.getCommand(command.name)
                    val slashCommandData = cascadeCommand?.commandData as SlashCommandData
                    builder.append("* </").append(command.name).append(':').append(command.id).append("> ")
                    for (option in slashCommandData.options) {
                        if (option.isRequired) {
                            builder.append("<")
                        } else {
                            builder.append("[")
                        }

                        builder.append(option.name)

                        if (option.isRequired) {
                            builder.append("> ")
                        } else {
                            builder.append("] ")
                        }
                    }
                    builder.append('\n')
                }
                context.replyInfo {
                    field {
                        name = "Core commands"
                        value = builder.toString()
                        inline = false
                    }
                    field {
                        name = "Custom Commands"
                        value = "TODO"
                        inline = false
                    }
                }
            }
        }
    }
}