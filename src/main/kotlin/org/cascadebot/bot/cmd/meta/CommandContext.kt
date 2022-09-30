package org.cascadebot.bot.cmd.meta

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class CommandContext(private val event: SlashCommandInteractionEvent) {

    fun reply(message: String) {
        event.reply(message).queue()
    }

}