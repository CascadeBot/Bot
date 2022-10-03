package org.cascadebot.bot.cmd

import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.cascadebot.bot.MessageType
import org.cascadebot.bot.cmd.meta.CommandArgs
import org.cascadebot.bot.cmd.meta.CommandContext
import org.cascadebot.bot.cmd.meta.RootCommand

class DashboardCommand : RootCommand("dashboard", "Provides a link to login to the dashboard") {

    override val commandData: CommandData = Commands.slash(name, description)

    override fun onCommand(context: CommandContext, args: CommandArgs) {
        // TODO: Add a link button™️
        context.replyEphemeral("Here is the link to the dashboard:", MessageType.INFO)
    }

}