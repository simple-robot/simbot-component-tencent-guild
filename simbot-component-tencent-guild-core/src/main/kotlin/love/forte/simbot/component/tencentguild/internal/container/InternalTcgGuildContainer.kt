/*
 *  Copyright (c) 2022-2022 ForteScarlet <ForteScarlet@163.com>
 *
 *  本文件是 simbot-component-tencent-guild 的一部分。
 *
 *  simbot-component-tencent-guild 是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是（按你的决定）任何以后版都可以。
 *
 *  发布 simbot-component-tencent-guild 是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 *
 *  你应该随程序获得一份 GNU 通用公共许可证的复本。如果没有，请看:
 *  https://www.gnu.org/licenses
 *  https://www.gnu.org/licenses/gpl-3.0-standalone.html
 *  https://www.gnu.org/licenses/lgpl-3.0-standalone.html
 *
 *
 */

package love.forte.simbot.component.tencentguild.internal.container

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import love.forte.simbot.ID
import love.forte.simbot.LoggerFactory
import love.forte.simbot.component.tencentguild.internal.TencentGuildComponentBotImpl
import love.forte.simbot.component.tencentguild.internal.TencentGuildImpl
import love.forte.simbot.component.tencentguild.internal.TencentGuildImpl.Companion.tencentGuildImpl
import love.forte.simbot.component.tencentguild.util.requestBy
import love.forte.simbot.literal
import love.forte.simbot.tencentguild.TencentGuildInfo
import love.forte.simbot.tencentguild.api.guild.GetBotGuildListApi
import love.forte.simbot.tencentguild.api.guild.GetGuildApi
import love.forte.simbot.tencentguild.requestBy
import love.forte.simbot.utils.item.Items
import love.forte.simbot.utils.item.Items.Companion.asItems
import love.forte.simbot.utils.item.effectedItemsByFlow
import love.forte.simbot.utils.runInBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal sealed class InternalTcgGuildContainer {
    abstract suspend fun get(id: String): TencentGuildImpl?
    abstract fun getBlocking(id: String): TencentGuildImpl?
    abstract suspend fun size(): Int
    abstract val size: Int
    abstract val values: Items<TencentGuildImpl>
}

internal inline val InternalTcgGuildContainer.memory: MemoryInternalTcgGuildContainer? get() = this as? MemoryInternalTcgGuildContainer

/**
 *
 * @author ForteScarlet
 */
internal class MemoryInternalTcgGuildContainer(
    val bot: TencentGuildComponentBotImpl,
    val container: ConcurrentMap<String, TencentGuildImpl> = ConcurrentHashMap(),
) : InternalTcgGuildContainer() {
    
    fun justGet(id: String): TencentGuildImpl? = container[id]
    
    override suspend fun get(id: String): TencentGuildImpl? {
        return justGet(id)
    }
    
    override fun getBlocking(id: String): TencentGuildImpl? {
        return justGet(id)
    }
    
    fun remove(id: String): TencentGuildImpl? {
        return container.remove(id)
    }
    
    override val size: Int
        get() = container.size
    
    override suspend fun size(): Int = size
    
    
    override val values: Items<TencentGuildImpl>
        get() = container.values.asItems()
    
    suspend inline fun computeAndGet(info: TencentGuildInfo): TencentGuildImpl {
        val idValue = info.id.literal
        return container[idValue]?.also {
            it.source = info
        } ?: run {
            val guildImpl = tencentGuildImpl(bot, info)
            container.compute(idValue) { _, current ->
                current?.also {
                    it.source = info
                } ?: guildImpl
            }!!
        }
    }
    
    
}


internal class ApiInternalTcgGuildContainer private constructor(
    val bot: TencentGuildComponentBotImpl,
    private val onRemove: (TencentGuildImpl) -> Unit,
) : InternalTcgGuildContainer() {
    private val lock = Mutex()
    private lateinit var cache: MutableMap<String, TencentGuildImpl> // = createLruCacheMap<String, TencentGuildImpl>(cacheMaxSize, onRemove)
    
    suspend fun initCache() {
        val allGuilds = bot.guildListFlow { batch, lastId, list ->
            logger.debug(
                "Init guild cache batch {} of the guild list, {} pieces of synchronized data, after id: {}",
                batch,
                list.size,
                lastId
            )
        }.buffer().map { it.toImpl() }.toList()
        
        cache = createLruCacheMap(allGuilds.size * 2, onRemove)
        allGuilds.forEach {
            cache[it.id.literal] = it
        }
    }
    
    private suspend fun getSync(id: String): TencentGuildImpl = lock.withLock {
        cache[id] ?: GetGuildApi(id.ID).requestBy(bot).toImpl().also {
            cache[id] = it
        }
    }
    
    override suspend fun get(id: String): TencentGuildImpl {
        return cache[id] ?: getSync(id)
    }
    
    override fun getBlocking(id: String): TencentGuildImpl {
        return cache[id] ?: runInBlocking { getSync(id) }
    }
    
    override suspend fun size(): Int {
        var counter = 1
        allGuildImpls().collect {
            counter++
        }
        return counter
    }
    
    override val size: Int
        get() = runInBlocking { size() }
    
    
    override val values: Items<TencentGuildImpl>
        get() = allGuildImpls()
    
    private suspend fun TencentGuildInfo.toImpl(): TencentGuildImpl {
        return tencentGuildImpl(bot, this)
    }
    
    private fun allGuildImpls(): Items<TencentGuildImpl> = bot.effectedItemsByFlow {
        bot.guildListFlow().map { tencentGuildImpl(bot, it) }
    }
    
    companion object {
        private val logger = LoggerFactory.getLogger<ApiInternalTcgGuildContainer>()
        
        internal suspend fun apiInternalTcgGuildContainer(
            bot: TencentGuildComponentBotImpl,
            onRemove: (TencentGuildImpl) -> Unit = {},
        ): ApiInternalTcgGuildContainer {
            return ApiInternalTcgGuildContainer(bot, onRemove).apply { initCache() }
        }
    }
}


internal inline fun TencentGuildComponentBotImpl.guildListFlow(crossinline onBatch: (batch: Int, lastId: ID?, list: List<TencentGuildInfo>) -> Unit = { _, _, _ -> }): Flow<TencentGuildInfo> =
    flow {
        var lastId: ID? = null
        var times = 1
        while (true) {
            val list = GetBotGuildListApi(after = lastId).requestBy(source)
            if (list.isEmpty()) break
            
            onBatch(times++, lastId, list)
            
            list.forEach { emit(it) }
            lastId = list[list.lastIndex].id
        }
    }