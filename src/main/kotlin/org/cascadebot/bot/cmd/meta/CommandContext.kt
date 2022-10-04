package org.cascadebot.bot.cmd.meta

import dev.minn.jda.ktx.messages.InlineEmbed
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.util.ref
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.cascadebot.bot.MessageType

class CommandContext(private val event: SlashCommandInteractionEvent) {

    val user by event.user.ref()

    // These can be non-null asserted as we don't process commands in DMs
    val member by event.member!!.ref()
    val guild by event.guild!!.ref()
    val jda = event.jda

    fun reply(message: String, messageType: MessageType = MessageType.NEUTRAL) {
        reply(false) {
            embeds += messageType.embed.apply {
                description = message
            }.build()
        }
    }

    fun replyEphemeral(message: String, messageType: MessageType = MessageType.NEUTRAL) {
        reply(true) {
            embeds += messageType.embed.apply {
                description = message
            }.build()
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

    fun reply(ephemeral: Boolean = false, messageBuilder: InlineMessage<MessageCreateData>.() -> Unit) {
        val data = InlineMessage(MessageCreateBuilder())
        messageBuilder(data)
        event.reply(data.build()).setEphemeral(ephemeral).queue()
    }

    private fun replyTypedEmbed(ephemeral: Boolean = false, messageType: MessageType, messageComponents: Collection<LayoutComponent>, builder: InlineEmbed.() -> Unit) {
        reply(ephemeral) {
            embeds += run {
                val embed = MessageType.INFO.embed
                builder(embed)
                embed.build()
            }
            messageComponents.forEach { components += it }
        }
    }

    fun replyInfo(ephemeral: Boolean = false, messageComponents: Collection<LayoutComponent> = listOf(), builder: InlineEmbed.() -> Unit) {
        replyTypedEmbed(ephemeral, MessageType.INFO, messageComponents, builder)
    }

    fun replyWarning(ephemeral: Boolean = false, messageComponents: Collection<LayoutComponent> = listOf(), builder: InlineEmbed.() -> Unit) {
        replyTypedEmbed(ephemeral, MessageType.WARNING, messageComponents, builder)
    }

    fun replyDanger(ephemeral: Boolean = false, messageComponents: Collection<LayoutComponent> = listOf(), builder: InlineEmbed.() -> Unit) {
        replyTypedEmbed(ephemeral, MessageType.DANGER, messageComponents, builder)
    }

    fun replySuccess(ephemeral: Boolean = false, messageComponents: Collection<LayoutComponent> = listOf(), builder: InlineEmbed.() -> Unit) {
        replyTypedEmbed(ephemeral, MessageType.SUCCESS, messageComponents, builder)
    }


}