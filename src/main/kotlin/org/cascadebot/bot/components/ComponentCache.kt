package org.cascadebot.bot.components

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import org.cascadebot.bot.utils.ChannelId
import org.cascadebot.bot.utils.ComponentId

class ComponentCache(private val maxPerChannel: Long) {

    val componentCache: LoadingCache<ChannelId, Cache<ComponentId, CascadeComponent>> = Caffeine.newBuilder().build {
        return@build Caffeine.newBuilder()
            .maximumSize(maxPerChannel)
            .build()
    }

    fun put(channelId: ChannelId, component: CascadeComponent) {
        componentCache.get(channelId).put(component.id, component)
    }

    fun remove(channelId: ChannelId, componentId: ComponentId) {
        componentCache.getIfPresent(channelId)?.invalidate(componentId)
    }

}