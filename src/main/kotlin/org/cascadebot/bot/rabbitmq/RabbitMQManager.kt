package org.cascadebot.bot.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.cascadebot.bot.Main
import org.cascadebot.bot.RabbitMQ
import kotlin.concurrent.getOrSet
import kotlin.system.exitProcess

class RabbitMQManager (config: RabbitMQ) {

    private val logger by SLF4J
    private val channels: ThreadLocal<Channel> = ThreadLocal()
    private val connectionFactory: ConnectionFactory = ConnectionFactory()
    private var connection: Connection

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

        setupQueues()
        setupConsumers()
    }

    private fun setupConsumers() {
        channel.basicConsume("meta", {consumerTag: String, delivery: Delivery ->
            val replyProps = AMQP.BasicProperties.Builder()
                .correlationId(delivery.properties.correlationId)
                .build()

            val response = mutableMapOf<String, JsonElement>()
            when (delivery.envelope.routingKey) {
                "meta.shard-count" -> {
                    response["shard-count"] = JsonPrimitive(Main.shardManager.shardsTotal)
                }
            }

            val json = Json.encodeToString(JsonObject(response))

            channel.basicPublish("", delivery.properties.replyTo, replyProps, json.toByteArray());
            channel.basicAck(delivery.envelope.deliveryTag, false);
        }, {_: String -> })
    }

    private fun setupQueues() {
        channel.queueDeclare("meta", true, false, false, mapOf())
        channel.queueBind("meta", "amq.direct", "meta.shard-count")
    }

}