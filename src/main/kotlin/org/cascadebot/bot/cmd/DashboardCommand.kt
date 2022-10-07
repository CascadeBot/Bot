package org.cascadebot.bot.cmd

import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.cascadebot.bot.Main
import org.cascadebot.bot.MessageType
import org.cascadebot.bot.cmd.meta.CommandArgs
import org.cascadebot.bot.cmd.meta.CommandContext
import org.cascadebot.bot.cmd.meta.RootCommand
import org.cascadebot.bot.utils.JwtUtil

class DashboardCommand : RootCommand("dashboard", "Provides a link to login to the dashboard") {

    override val commandData: CommandData = Commands.slash(name, description)

    override fun onCommand(context: CommandContext, args: CommandArgs) {
        // TODO: Add a link button™️
        val dashboardConfig = Main.config.dashboard
        val jwt = JwtUtil.createLoginJwt(context.user.idLong)
        context.reply(
            "Here is the link to the dashboard: ${dashboardConfig.dashboardBaseUrl.trimEnd('/')}/login/bot?token=${jwt}",
            MessageType.INFO
        )
    }

}