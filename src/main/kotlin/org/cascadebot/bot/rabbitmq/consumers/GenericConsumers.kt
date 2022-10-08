package org.cascadebot.bot.rabbitmq.consumers

import com.rabbitmq.client.CancelCallback

class NoOpCancelCallback : CancelCallback {

    override fun handle(consumerTag: String) {
        return // NOOP
    }

}