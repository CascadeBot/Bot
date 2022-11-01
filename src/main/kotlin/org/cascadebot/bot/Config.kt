package org.cascadebot.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.Masked
import com.sksamuel.hoplite.addEnvironmentSource
import com.sksamuel.hoplite.addFileSource
import dev.minn.jda.ktx.util.SLF4J
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlin.io.path.Path
import kotlin.system.exitProcess

sealed class Sharding {
    data class Total(val total: Int = -1) : Sharding()
    data class MinMax(val total: Int, val min: Int, val max: Int) : Sharding()
}

data class Discord(
    val token: String,
    val shards: Sharding?,
    val supportServerInvite: String = "https://discord.gg/P23GZFB" // TODO: Probably want to add this to the planned resources file
)

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

sealed class DashboardEncryption {

    abstract val privateKey: ECPrivateKey
    abstract val publicKey: ECPublicKey

    val logger by SLF4J("DashboardEncryption")

    data class Generate(val generate: Boolean) : DashboardEncryption() {

        override val privateKey: ECPrivateKey
        override val publicKey: ECPublicKey

        init {
            val keyPair = if (Files.exists(Path("generated.pem"))) {
                if (!Files.exists(Path("generated.pub"))) {
                    logger.error("Generated private key exists but no public key found. Delete private key to force regeneration or restore public key file.")
                    throw IllegalArgumentException()
                }

                val keys = Key("generated.pem", "generated.pub")
                val publicKeyEncoded =
                    Base64.getEncoder().encodeToString(keys.publicKey.encoded).chunked(64).joinToString("\n")
                val publicKeyPem = "-----BEGIN PUBLIC KEY-----\n$publicKeyEncoded\n-----END PUBLIC KEY-----"

                logger.info("Using existed generated public key:\n$publicKeyPem")
                KeyPair(keys.publicKey, keys.privateKey)
            } else {
                val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("EC")
                kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
                val keyPair: KeyPair = kpg.generateKeyPair()

                val privateKeyEncoded =
                    Base64.getEncoder().encodeToString(keyPair.private.encoded).chunked(64).joinToString("\n")
                val publicKeyEncoded =
                    Base64.getEncoder().encodeToString(keyPair.public.encoded).chunked(64).joinToString("\n")

                val privateKeyPem = "-----BEGIN PRIVATE KEY-----\n$privateKeyEncoded\n-----END PRIVATE KEY-----"
                val publicKeyPem = "-----BEGIN PUBLIC KEY-----\n$publicKeyEncoded\n-----END PUBLIC KEY-----"

                File("generated.pem").writeText(privateKeyPem, StandardCharsets.UTF_8)
                File("generated.pub").writeText(publicKeyPem, StandardCharsets.UTF_8)

                logger.info("Public and private key pair have been generated and stored in generated.pub and generated.pem respectively.")
                logger.info("Generated public key:\n$publicKeyPem")

                KeyPair(keyPair.public as ECPublicKey, keyPair.private as ECPrivateKey)
            }

            this.privateKey = keyPair.private as ECPrivateKey
            this.publicKey = keyPair.public as ECPublicKey


        }

    }

    data class Key(val privateKeyFile: String, val publicKeyFile: String) : DashboardEncryption() {

        override val privateKey: ECPrivateKey
        override val publicKey: ECPublicKey

        init {

            val privateFile = File(privateKeyFile)
            val publicFile = File(publicKeyFile)

            val privateKeyBytes = with(privateFile) {
                if (!isFile || !exists()) {
                    logger.error("The file '{}' doesn't exist.", absolutePath)
                    exitProcess(1)
                }

                val key = String(Files.readAllBytes(toPath()), StandardCharsets.UTF_8)

                if (key.startsWith("-----BEGIN EC PRIVATE KEY-----")) {
                    logger.error("You are using the legacy OpenSSL key format for the dashboard private key. Please convert the private key to PKCS8 using the command below.")
                    logger.error("openssl pkey -in legacy.pem -out pkcs8.pem")
                    exitProcess(1)
                }

                val privateKeyPEM = key
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace(Regex("\r?\n"), "")
                    .replace("-----END PRIVATE KEY-----", "")

                val encoded: ByteArray = try {
                    Base64.getDecoder().decode(privateKeyPEM)
                } catch (e: IllegalArgumentException) {
                    logger.error("Could not decode Base64 in PEM file. Error: {}", e.message)
                    exitProcess(1)
                }

                encoded
            }

            val publicKeyBytes = with(publicFile) {
                if (!isFile || !exists()) {
                    logger.error("The file '{}' doesn't exist.", absolutePath)
                    exitProcess(1)
                }

                val key = String(Files.readAllBytes(toPath()), StandardCharsets.UTF_8)

                val publicKeyPEM = key
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace(Regex("\r?\n"), "")
                    .replace("-----END PUBLIC KEY-----", "")

                val encoded: ByteArray = try {
                    Base64.getDecoder().decode(publicKeyPEM)
                } catch (e: IllegalArgumentException) {
                    logger.error("Could not decode Base64 in PEM file. Error: {}", e.message)
                    exitProcess(1)
                }

                encoded
            }


            try {
                val kf = KeyFactory.getInstance("EC")
                val privateKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
                val publicKeySpec = X509EncodedKeySpec(publicKeyBytes)
                privateKey = kf.generatePrivate(privateKeySpec) as ECPrivateKey
                publicKey = kf.generatePublic(publicKeySpec) as ECPublicKey

                logger.info("Loaded private key successfully from '{}'", privateFile.absolutePath)
                logger.info("Loaded public key successfully from '{}'", publicFile.absolutePath)
            } catch (e: Exception) {
                logger.error("Could not read private key", e)
                exitProcess(1)
            }
        }
    }

}

data class Dashboard(
    val baseUrl: String = "http://localhost:3000",
    val encryption: DashboardEncryption = DashboardEncryption.Key("dashboard_login.pem", "dashboard_login.pub")
)

data class Values(val maxComponentsCachedPerChannel: Long = 50L)

data class Config(
    val database: Database,
    val discord: Discord,
    val rabbitMQ: RabbitMQ?,
    val development: DevelopmentSettings?,
    val dashboard: Dashboard = Dashboard(),
    val values: Values = Values()
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