package org.cascadebot.bot.db

import jakarta.persistence.Entity
import org.cascadebot.bot.Database
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.hibernate.dialect.PostgreSQLDialect
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider
import org.postgresql.Driver
import org.reflections.Reflections
import java.util.Properties
import kotlin.reflect.jvm.jvmName

class PostgresManager(config: Database) {

    private val sessionFactory: SessionFactory

    init {
        sessionFactory = createConfig(config).buildSessionFactory()
    }

    private fun createConfig(config: Database): Configuration {
        val dbConfig = Configuration()

        val entityReflections = Reflections("org.cascadebot.bot.db.entities")
        val classes: Set<Class<*>> = entityReflections.getTypesAnnotatedWith(Entity::class.java)

        classes.forEach { dbConfig.addAnnotatedClass(it) }

        // Add package to read package-level declarations and metadata
        dbConfig.addPackage("org.cascadebot.bot.db.entities")

        val hibernateProps = Properties()
        hibernateProps["hibernate.dialect"] = PostgreSQLDialect::class.jvmName
        hibernateProps["hibernate.connection.provider_class"] = HikariCPConnectionProvider::class.jvmName
        hibernateProps["hibernate.connection.driver_class"] = Driver::class.jvmName
        hibernateProps["hibernate.hikari.maximumPoolSize"] = "20"
        hibernateProps["hibernate.types.print.banner"] = "false"
        hibernateProps["hibernate.connection.url"] = "jdbc:" + config.url.removePrefix("jdbc:")

        if (config.username != null) {
            hibernateProps["hibernate.connection.username"] = config.username
        }

        if (config.password != null) {
            hibernateProps["hibernate.connection.password"] = config.password.value
        }

        dbConfig.properties = hibernateProps
        return dbConfig
    }

}
