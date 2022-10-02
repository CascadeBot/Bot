package org.cascadebot.bot.events

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.cascadebot.bot.Main
import org.cascadebot.bot.MessageType
import org.cascadebot.bot.cmd.meta.CommandArgs
import org.cascadebot.bot.cmd.meta.CommandContext

class InteractionListener : ListenerAdapter() {

    private val logger by SLF4J

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!event.isFromGuild) {
            return
        }

        val context = CommandContext(event)
        val args = CommandArgs(event.options.groupBy { it.name })

        if (event.isGlobalCommand) {
            val command = Main.commandManager.getCommand(event.commandPath);
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
        } else {
            // TODO: Process custom commands
        }
    }

}