package org.cascadebot.bot.utils

import com.rabbitmq.client.AMQP

object RabbitMQUtil {

    fun replyPropsFromRequest(properties: AMQP.BasicProperties): AMQP.BasicProperties {
        return AMQP.BasicProperties.Builder()
            .correlationId(properties.correlationId)
            .build()
    }

}