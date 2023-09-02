package org.cascadebot.bot.cmd

import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.cascadebot.bot.Main
import org.cascadebot.bot.cmd.meta.CommandArgs
import org.cascadebot.bot.cmd.meta.CommandContext
import org.cascadebot.bot.cmd.meta.RootCommand
import org.cascadebot.bot.components.CascadeActionRow
import org.cascadebot.bot.components.CascadeLinkButton
import org.cascadebot.bot.components.ComponentContainer
import org.cascadebot.bot.utils.JwtUtil

class DashboardCommand : RootCommand("dashboard", "Provides a link to login to the dashboard") {

    override val commandData: CommandData = Commands.slash(name, description)

    override fun onCommand(context: CommandContext, args: CommandArgs) {
        val dashboardConfig = Main.config.dashboard
        val jwt = JwtUtil.createLoginJwt(context.user.idLong)

        // TODO Change URL depending on whether the user needs to go through the OAuth flow or not
        val loginUrl = "${dashboardConfig.baseUrl.trimEnd('/')}/login/bot?token=${jwt}"

        val row = CascadeActionRow()
        row.addComponent(CascadeLinkButton.of(loginUrl, "Open Dashboard"))

        context.replyInfo(true, ComponentContainer().apply { addRow(row) }) {
            description = """Click the button to open the dashboard.
                |You may be asked to "Authorize" the bot to login.
            """.trimMargin()
        }
    }

}