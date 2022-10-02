package org.cascadebot.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.Masked
import com.sksamuel.hoplite.addEnvironmentSource
import com.sksamuel.hoplite.addFileSource
import dev.minn.jda.ktx.util.SLF4J

sealed class Sharding {
    data class Total(val total: Int = -1) : Sharding()
    data class MinMax(val total: Int, val min: Int, val max: Int) : Sharding()
}

data class Discord(val token: String, val shards: Sharding?)

data class Database(val url: String, val username: String? = null, val password: Masked? = null)

data class DevelopmentSettings(
    val registerCommandsOnBoot: Boolean = false,
    val debugLogs: Boolean = false,
)

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
    val database: Database,
    val discord: Discord,
    val rabbitMQ: RabbitMQ?,
    val development: DevelopmentSettings?
) {

    companion object {

        private val logger by SLF4J("Config")

        fun load(file: String = "config.yml"): ConfigResult<Config> {

            val configLoaderBuilder = ConfigLoaderBuilder.default()
                .addFileSource(file, optional = true)
                .addEnvironmentSource(allowUppercaseNames = true)
                .build()

            val configResult = configLoaderBuilder.loadConfig<Config>()

            if (configResult.isValid()) {
                logger.debug("Loaded config with options:\n{}", configResult.getUnsafe().toString())
            }

            return configResult
        }
    }

}