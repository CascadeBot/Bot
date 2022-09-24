package org.cascadebot.bot.db

import jakarta.persistence.Entity
import org.cascadebot.bot.Database
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.reflections.Reflections
import java.util.Properties

class PostgresManager(config: Database) {

    val sessionFactory: SessionFactory

    init {
        sessionFactory = getConfig(config).buildSessionFactory()
    }

    private fun getConfig(config: Database): Configuration {
        val dbConfig = Configuration()

        val entityReflections = Reflections("org.cascadebot.bot.db.entities")
        val classes: Set<Class<*>> = entityReflections.getTypesAnnotatedWith(Entity::class.java)

        classes.forEach { dbConfig.addAnnotatedClass(it) }

        dbConfig.addPackage("org.cascadebot.bot.db.entities")

        val hibernateProps = Properties()
        hibernateProps["hibernate.dialect"] = "org.hibernate.dialect.PostgreSQLDialect"
        hibernateProps["hibernate.connection.provider_class"] =
            "org.hibernate.hikaricp.internal.HikariCPConnectionProvider"
        hibernateProps["hibernate.connection.driver_class"] = "org.postgresql.Driver"
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
