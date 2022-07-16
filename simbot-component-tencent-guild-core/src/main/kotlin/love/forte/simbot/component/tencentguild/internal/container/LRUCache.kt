package love.forte.simbot.component.tencentguild.internal.container

import love.forte.simbot.Simbot


private const val DEFAULT_CACHE_INITIAL_CAPACITY = 10


private const val DEFAULT_CACHE_LOAD_FACTOR = 0.75f

internal fun <K, V> createLruCacheMap(maxSize: Int, onRemove: (V) -> Unit = {}): LinkedHashMap<K, V> {
    return LRUCache(maxSize, onRemove)
}

private class LRUCache<K, V>(
    private val maxSize: Int,
    private val onRemove: (V) -> Unit,
) : LinkedHashMap<K, V>(DEFAULT_CACHE_INITIAL_CAPACITY, DEFAULT_CACHE_LOAD_FACTOR, true) {
    init {
        Simbot.require(maxSize > 0) { "Size must > 0" }
    }
    
    override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
        return (size > maxSize).also {
            if (it) {
                onRemove(eldest.value)
            }
        }
    }
}