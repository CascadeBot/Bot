package org.cascadebot.bot.cmd.meta

import dev.minn.jda.ktx.messages.EmbedBuilder
import dev.minn.jda.ktx.messages.InlineEmbed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.util.ref
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.cascadebot.bot.MessageType

class CommandContext(private val event: SlashCommandInteractionEvent) {

    val user by event.user.ref()

    // These can be non-null asserted as we don't process commands in DMs
    val member by event.member!!.ref()
    val guild by event.guild!!.ref()
    val jda = event.jda

    fun reply(message: String, messageType: MessageType = MessageType.NEUTRAL) {
        replyEmbed(false, messageType) {
            description = message
        }
        user
    }

    fun replyEphemeral(message: String, messageType: MessageType = MessageType.NEUTRAL) {
        replyEmbed(true, messageType) {
            description = message
        }
    }

    fun reply(vararg embed: MessageEmbed) {
        val data = MessageCreateBuilder().apply {
            addEmbeds(*embed)
        }
        event.reply(data.build()).queue()
    }

    fun replyEphemeral(vararg embeds: MessageEmbed) {
        val data = MessageCreate {
            this.embeds += embeds.toList()
        }
        event.reply(data).setEphemeral(true).queue()
    }

    private fun replyEmbed(ephemeral: Boolean = false, messageType: MessageType, builder: InlineEmbed.() -> Unit) {
        val embed = EmbedBuilder {
            color = messageType.color?.rgb
        }
        builder(embed)
        event.replyEmbeds(embed.build()).setEphemeral(ephemeral).queue()
    }

    fun replyInfo(ephemeral: Boolean = false, builder: InlineEmbed.() -> Unit) {
        replyEmbed(ephemeral, MessageType.INFO, builder)
    }

    fun replyWarning(ephemeral: Boolean = false, builder: InlineEmbed.() -> Unit) {
        replyEmbed(ephemeral, MessageType.WARNING, builder)
    }

    fun replyDanger(ephemeral: Boolean = false, builder: InlineEmbed.() -> Unit) {
        replyEmbed(ephemeral, MessageType.DANGER, builder)
    }

    fun replySuccess(ephemeral: Boolean = false, builder: InlineEmbed.() -> Unit) {
        replyEmbed(ephemeral, MessageType.SUCCESS, builder)
    }


}