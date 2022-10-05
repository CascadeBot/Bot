package org.cascadebot.bot.components

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import org.cascadebot.bot.utils.ChannelId
import org.cascadebot.bot.utils.ComponentId
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class ComponentCache(private val maxPerChannel: Long) {

    private val cacheLoader: CacheLoader<ChannelId, Cache<ComponentId, CascadeComponent>> = CacheLoader { channelId ->
        return@CacheLoader Caffeine.newBuilder()
            .maximumSize(maxPerChannel)
            .expireAfterAccess(1.hours.toJavaDuration())
            .build()
    }

    // Cache within a cache. Inner cache is created by the cache loader function
    val componentCache: LoadingCache<ChannelId, Cache<ComponentId, CascadeComponent>> = Caffeine.newBuilder()
        .expireAfterAccess(1.hours.toJavaDuration())
        .build(cacheLoader)

    fun put(channelId: ChannelId, component: CascadeComponent) {
        componentCache.get(channelId).put(component.id, component)
    }

    fun remove(channelId: ChannelId, componentId: ComponentId) {
        componentCache.getIfPresent(channelId)?.let {
            it.invalidate(componentId)
            // If channel cache empty, remove it
            if (it.estimatedSize() == 0L) {
                componentCache.invalidate(channelId)
            }
        }

    }

}