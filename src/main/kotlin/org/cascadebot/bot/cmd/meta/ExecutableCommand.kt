package org.cascadebot.bot.cmd.meta

import net.dv8tion.jda.api.interactions.commands.build.CommandData

abstract class ExecutableCommand(val name: String, val description: String) {

    abstract val commandData: CommandData

    abstract fun onCommand(context: CommandContext, args: CommandArgs, data: Any)

}