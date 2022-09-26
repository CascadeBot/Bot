package org.cascadebot.bot.db

import dev.minn.jda.ktx.util.SLF4J
import jakarta.persistence.Entity
import org.cascadebot.bot.Database
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.hibernate.dialect.PostgreSQLDialect
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider
import org.postgresql.Driver
import org.reflections.Reflections
import java.util.Properties
import kotlin.reflect.jvm.jvmName

class PostgresManager(config: Database) {

    private val logger by SLF4J
    private val sessionFactory: SessionFactory
    val sessions: ThreadLocal<Session> = ThreadLocal()

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

    fun <T : Any> transaction(work: Session.() -> T?): T? {
        var session = sessions.get()

        if (session == null || !session.isOpen) {
            sessionFactory.openSession().apply {
                sessions.set(this)
                session = this
            }
        }

        return createTransaction(session, work)
    }

    private fun <T : Any> createTransaction(session: Session, work: Session.() -> T?): T? {
        try {
            session.transaction.timeout = 3
            session.transaction.begin()

            val value = work(session);

            session.transaction.commit()
            return value;
        } catch (e: RuntimeException) {
            session.transaction.rollback()
            throw e; // TODO: Or display error?
        }
    }

}
