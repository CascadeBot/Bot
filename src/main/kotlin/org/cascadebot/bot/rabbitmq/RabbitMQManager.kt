package org.cascadebot.bot.rabbitmq

import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import dev.minn.jda.ktx.util.SLF4J
import org.cascadebot.bot.RabbitMQ
import org.cascadebot.bot.rabbitmq.consumers.MetaConsumer
import org.cascadebot.bot.rabbitmq.consumers.ResourceConsumer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.getOrSet
import kotlin.system.exitProcess

class RabbitMQManager (config: RabbitMQ) {

    private val logger by SLF4J
    private val channels: ThreadLocal<Channel> = ThreadLocal()
    private val connectionFactory: ConnectionFactory = ConnectionFactory()
    private var connection: Connection

    private val isSetup = AtomicBoolean(false)

    val channel: Channel
        get() {
            if (!connection.isOpen) {
                try {
                    connection = connectionFactory.newConnection()
                } catch (e: Exception) {
                    logger.error("Error setting up RabbitMQ connection", e)
                }
            }
            return channels.getOrSet {
                logger.info("Opening new channel on thread '{}'", Thread.currentThread().name)
                try {
                    connection.createChannel()
                } catch (e: Exception) {
                    logger.error("Could not open channel on thread '${Thread.currentThread().name}'", e)
                    throw e
                }
            }
        }

    init {
        logger.info("Setting up RabbitMQ Connection with config: {}", config)
        when (config) {
            is RabbitMQ.Individual -> {
                connectionFactory.apply {
                    host = config.hostname
                    port = config.port
                    username = config.username
                    password = config.password.value
                    virtualHost = config.virtualHost
                }
            }
            is RabbitMQ.URL -> {
                connectionFactory.setUri(config.url)
            }
        }
        try {
            connection = connectionFactory.newConnection()
        } catch (e: Exception) {
            logger.error("Error setting up RabbitMQ connection", e)
            exitProcess(1)
        }
    }

    fun setupRabbitMQ() {
        if (isSetup.getAndSet(true)) return
        val globalObjectNames = setupGlobalObjects()
        setupConsumers(globalObjectNames)
    }

    private fun setupConsumers(globalObjectNames: GlobalObjectNames) {
        channel.basicConsume("meta", MetaConsumer(channel))
        channel.basicConsume("resource", ResourceConsumer(channel))
//      Leaving this here as it will likely be needed in the future but not currently required.
//        channel.basicConsume(globalObjectNames.broadcastQueueName, BroadcastConsumer(channel))
    }

    private fun setupGlobalObjects(): GlobalObjectNames {
        channel.exchangeDeclare("bot.broadcast", BuiltinExchangeType.FANOUT, true)

        val broadcastQueueName = channel.queueDeclare().queue

        // Bind to the broadcast exchange - Routing key can be empty as a fanout exchange ignores the routing key
        channel.queueBind(broadcastQueueName, "bot.broadcast", "")

        channel.queueDeclare("meta", true, false, false, mapOf())
        channel.queueDeclare("resource", true, false, false, mapOf())

        channel.queueBind("meta", "amq.direct", "meta")
        channel.queueBind("resource", "amq.direct", "resource")

        return GlobalObjectNames(broadcastQueueName)
    }

    data class GlobalObjectNames(val broadcastQueueName: String)

}
