package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer
import org.cascadebot.bot.rabbitmq.actions.Processor
import org.cascadebot.bot.rabbitmq.objects.ChannelResponse
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.utils.PaginationUtil

class ChannelWithThreadsProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        context: RabbitMQContext,
        guild: Guild
    ): RabbitMQResponse<*> {
        val channel = ChannelUtils.validateAndGetChannel(body, guild)

        if (channel == null) {
            return CommonResponses.CHANNEL_NOT_FOUND
        }

        if (parts.isEmpty()) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        channel as IThreadContainer

        return when {
            checkAction(parts, "list") -> listThreads(body, channel)
            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun listThreads(
        body: ObjectNode,
        channel: IThreadContainer
    ): RabbitMQResponse<PaginationUtil.PaginationResult<ChannelResponse>> {
        val params = PaginationUtil.parsePaginationParameters(body)
        return RabbitMQResponse.success(params.paginate(channel.threadChannels.map {
            ChannelResponse.fromThread(
                it
            )
        }))
    }
}