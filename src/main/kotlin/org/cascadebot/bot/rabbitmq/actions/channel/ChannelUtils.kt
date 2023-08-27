package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel

class ChannelUtils {

    companion object {

        fun validateAndGetChannel(body: ObjectNode, guild: Guild): GuildChannel? {
            if (!body.has("channel")) {
                return null
            }

            val channelNode = body.get("channel")
            val typeStr = channelNode.get("type").asText()
            val id = channelNode.get("id").asLong()

            val type = ChannelType.entries.firstOrNull { it.name.equals(typeStr, true) }

            if (type == null) {
                return null
            }

            return guild.getGuildChannelById(type, id)
        }
    }

}