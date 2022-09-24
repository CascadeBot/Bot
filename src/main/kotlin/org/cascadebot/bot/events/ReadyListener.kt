package org.cascadebot.bot.events

import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.cascadebot.bot.Main

class ReadyListener(private val main: Main): ListenerAdapter() {

    override fun onReady(event: ReadyEvent) {
        main.logger.info("JDA shard " + event.jda.shardInfo.shardId + " logged in!") // TODO this is here to test that jda is working, we probably don't need this
    }
}