package org.cascadebot.bot.rabbitmq.consumers

import com.rabbitmq.client.Channel
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.utils.createJsonObject

class MetaConsumer(channel: Channel) : ErrorHandledConsumer(channel) {

    override fun onDeliver(
        consumerTag: String,
        context: RabbitMQContext,
        body: String
    ) {
        if (!assertReplyTo(context)) return

        val responseProps = mutableListOf<Pair<String, Any?>>()

        val property = context.properties.headers["action"].toString()

        when (property) {
            "shard_count" -> {
                responseProps.add("shard_count" to Main.shardManager.shardsTotal)
            }

            else -> {
                RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidProperty,
                    "The property '$property' is invalid"
                ).sendAndAck(context)
                return
            }
        }

        val responseObject = RabbitMQResponse.success(createJsonObject(responseProps))
        responseObject.sendAndAck(context)
    }

}