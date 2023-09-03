package org.cascadebot.bot.db

import dev.minn.jda.ktx.util.SLF4J
import jakarta.persistence.Entity
import org.cascadebot.bot.Database
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.Transaction
import org.hibernate.cfg.Configuration
import org.hibernate.dialect.PostgreSQLDialect
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider
import org.postgresql.Driver
import org.reflections.Reflections
import java.util.Properties
import kotlin.reflect.jvm.jvmName
import kotlin.system.exitProcess

val urlCredentialsRegex = Regex("://([^@/]*)@")

class PostgresManager(config: Database) {

    private val logger by SLF4J
    private val sessionFactory: SessionFactory

    init {
        sessionFactory = createConfig(config).buildSessionFactory()
    }

    private fun createConfig(config: Database): Configuration {
        logger.info("Setting up Postgres Database with config: {}", config)
        val dbConfig = Configuration()

        val entityReflections = Reflections("org.cascadebot.bot.db.entities")
        val classes: Set<Class<*>> = entityReflections.getTypesAnnotatedWith(Entity::class.java)

        classes.forEach { dbConfig.addAnnotatedClass(it) }

        // Add package to read package-level declarations and metadata
        dbConfig.addPackage("org.cascadebot.bot.db.entities")

        var url = config.url.removePrefix("jdbc:")
        var username = config.username
        var password = config.password?.value

        // This section allows the use of username and password in the URL
        // Hikari will not accept a URL with the userinfo, so we parse it out, remove it from the url
        // and pass it in separately
        val matches = urlCredentialsRegex.find(url)
        if (matches != null) {
            val (userInfo) = matches.destructured
            val splitUserInfo = userInfo.split(":")
            username = splitUserInfo[0]
            if (splitUserInfo.getOrNull(1) != null) {
                password = splitUserInfo[1]
            }
            url = url.replaceFirst("$userInfo@", "")
        }

        val parseResult = Driver.parseURL("jdbc:$url", null)
        if (parseResult == null) {
            logger.error("Postgres connection URL invalid. Please make sure the format postgresql://host:port/database is followed!")
            exitProcess(1)
        }

        val hibernateProps = Properties()
        hibernateProps["hibernate.dialect"] = PostgreSQLDialect::class.jvmName
        hibernateProps["hibernate.connection.provider_class"] = HikariCPConnectionProvider::class.jvmName
        hibernateProps["hibernate.connection.driver_class"] = Driver::class.jvmName
        hibernateProps["hibernate.hikari.maximumPoolSize"] = "20"
        hibernateProps["hibernate.types.print.banner"] = "false"
        hibernateProps["hibernate.connection.url"] = "jdbc:$url"

        if (username != null) {
            hibernateProps["hibernate.connection.username"] = username
        }

        if (password != null) {
            hibernateProps["hibernate.connection.password"] = password
        }

        dbConfig.properties = hibernateProps
        return dbConfig
    }

    fun <T : Any?> transaction(work: Session.() -> T): T {
        return sessionFactory.openSession().use { session ->
            val transaction: Transaction = session.beginTransaction()
            transaction.timeout = 3

            try {
                val value = work(session)

                transaction.commit();

                return@use value
            } catch (e: RuntimeException) {
                transaction.rollback()
                throw e // TODO: Or display error?
            }
        }
    }

}
