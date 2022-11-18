package org.cascadebot.bot.utils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import org.slf4j.LoggerFactory
import java.io.InputStream

/**
 * Provides utility methods for interfacing with Logback
 */
object LogbackUtil {

    /**
     * Gets the root logger defined by [Logger.ROOT_LOGGER_NAME].
     *
     * @return The root logger.
     * @see LoggerFactory.getLogger
     */
    val rootLogger: Logger
        get() = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

    /**
     * Gets the Logback [LoggerContext] from the SLF4J's [LoggerFactory].
     *
     * @return The LogBack logger context.
     */
    val loggerContext: LoggerContext
        get() = LoggerFactory.getILoggerFactory() as LoggerContext

    /**
     * Sets the level of the root logger defined by [Logger.ROOT_LOGGER_NAME].
     *
     * @param level Which level to set for the root logger.
     * @see Logger.setLevel
     * @see LogbackUtil.rootLogger
     */
    fun setRootLoggerLevel(level: Level) {
        val root = rootLogger
        root.level = level
    }

    /**
     * Sets the level for the logger specified by the provided name.
     *
     * @param name  The name of the logger to set the level for.
     * @param level What level to set the logger at.
     * @see Logger.setLevel
     * @see LogbackUtils.getLogger
     */
    fun setLoggerLevel(name: String, level: Level?) {
        val logger = getLogger(name)
        logger.level = level
    }

    /**
     * Gets a logger by the specified name from SLF4J's [LoggerFactory].
     *
     *
     * *Warning: If the logger does not exist the LoggerFactory will just create a new logger. No exception will be thrown.*
     *
     * @param name The name of the logger to retrieve.
     * @return The requested logger.
     * @see LoggerFactory.getLogger
     */
    fun getLogger(name: String): Logger {
        return LoggerFactory.getLogger(name) as Logger
    }

    /**
     * Sets the specified appender level by disabling all other [ThresholdFilter]s and programmatically adding
     * a threshold filter set at the specified level.
     *
     * @param name  The name of the appender to add to change the level of.
     * @param level The level to set the appender to.
     * @throws IllegalArgumentException If the provided name does not map to a assigned appender.
     */
    fun setAppenderLevel(name: String, level: Level) {
        val root = rootLogger
        val appender = root.getAppender(name)
        if (appender != null) {
            val filter = ThresholdFilter()
            for (eventFilter in appender.copyOfAttachedFiltersList) {
                (eventFilter as? ThresholdFilter)?.stop()
            }
            filter.setLevel(level.toString())
            appender.addFilter(filter)
        } else {
            throw IllegalArgumentException("The provided name does not have a defined appender")
        }
    }

    /**
     * Reloads the logback configuration from the specified [InputStream].
     *
     * @param inputStream The input stream to read the configuration from.
     * @throws JoranException If the configurator encounters an issue with the config.
     */
    @JvmOverloads
    @Throws(JoranException::class)
    fun reloadFromConfig(inputStream: InputStream? = LogbackUtil::class.java.getResourceAsStream("/logback.xml")) {
        require(inputStream != null) { "Input Stream is null!" }
        val context = loggerContext
        context.reset()
        val configurator = JoranConfigurator()
        configurator.context = context
        configurator.doConfigure(inputStream)
    }


}