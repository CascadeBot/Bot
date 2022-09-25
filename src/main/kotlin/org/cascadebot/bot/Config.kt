package org.cascadebot.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.Masked
import com.sksamuel.hoplite.addEnvironmentSource
import com.sksamuel.hoplite.addFileSource

sealed class Sharding {
    data class Total(val total: Int = -1) : Sharding()
    data class MinMax(val total: Int, val min: Int, val max: Int) : Sharding()
}

data class Bot(val token: String, val shards: Sharding?)

data class Database(val url: String, val username: String? = null, val password: Masked? = null)

sealed class RabbitMQ {
    data class URL(val url: String) : RabbitMQ()
    data class Individual(
        val username: String,
        val password: Masked,
        val hostname: String,
        val port: Int = 5672,
        val virtualHost: String = "/",
    ) : RabbitMQ()
}

data class Config(
    val debug: Boolean = false,
    val database: Database,
    val botConfig: Bot,
    val rabbitMQ: RabbitMQ
) {

    companion object {

        fun load(file: String = "config.yml"): ConfigResult<Config> {
            return ConfigLoaderBuilder.default()
                .addFileSource(file, optional = true)
                .addEnvironmentSource(allowUppercaseNames = true)
                .build()
                .loadConfig()
        }
    }

}