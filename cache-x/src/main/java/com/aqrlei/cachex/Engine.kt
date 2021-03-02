package com.aqrlei.cachex

import com.aqrlei.cachex.lru.memory.ActiveResource
import com.aqrlei.cachex.lru.memory.ByteResource
import com.aqrlei.cachex.lru.memory.cache.LruResourceCache

/**
 * created by AqrLei on 2/27/21
 */
class Engine(
    private val activeResource: ActiveResource,
    private val cache: LruResourceCache
) : ByteResource.ResourceListener, CacheModel() {

    override fun onResourceReleased(key: Key, resource: ByteResource) {
        activeResource.deactivate(key)
        if (resource.isMemoryCacheable) {
            cache.put(key, resource)
        }
    }

    fun load(
        key: Key,

        callback: ResourceCallback): Any? {
        val memoryResource: ByteResource?
        synchronized(this) {
            memoryResource = loadFromMemory(key)
            if (memoryResource == null) {
                cacheJob.setBackgroundBlock()
                cacheJob.start()
                return null
            }
        }

        callback.onResourceReady(memoryResource!!, DataResource.MEMORY_CACHE)
        return null
    }

    private fun loadFromMemory(key: Key): ByteResource? {

        val active = loadFromActiveResource(key)
        if (active != null) return active

        val cached = loadFromCache(key)
        if (cached != null) return cached

        return null
    }

    private fun loadFromActiveResource(key: Key) = activeResource.get(key)

    private fun loadFromCache(key: Key): ByteResource? {
        val cached = cache.remove(key)
        if (cached != null) {
            activeResource.activate(key, cached)
        }
        return cached
    }
}