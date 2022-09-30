package org.cascadebot.bot.events

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.cascadebot.bot.Main
import org.cascadebot.bot.cmd.meta.CommandArgs
import org.cascadebot.bot.cmd.meta.CommandContext

class InteractionListener : ListenerAdapter() {

    private val logger by SLF4J

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val args = CommandArgs(event.options.groupBy { it.name })
        if (event.isGlobalCommand) {
            logger.info(event.commandPath)
            logger.info(Main.commandManager.commands.map { it.first.path.joinToString(", ", "[", "]")}.joinToString { "\n" })
            Main.commandManager.getCommand(event.commandPath)?.onCommand(
                CommandContext(event),
                args,
                ""
            )
        }
    }

}