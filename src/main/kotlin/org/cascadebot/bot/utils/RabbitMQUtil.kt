package org.cascadebot.bot.utils

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.impl.AMQBasicProperties

object RabbitMQUtil {

    fun propsFromCorrelationId(properties: AMQP.BasicProperties): AMQP.BasicProperties? {
        return AMQP.BasicProperties.Builder()
            .correlationId(properties.correlationId)
            .build()
    }

}