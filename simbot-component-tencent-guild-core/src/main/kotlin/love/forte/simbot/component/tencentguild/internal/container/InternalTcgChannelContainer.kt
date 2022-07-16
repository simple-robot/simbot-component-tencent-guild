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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import love.forte.simbot.ID
import love.forte.simbot.component.tencentguild.internal.TencentChannelCategoryImpl
import love.forte.simbot.component.tencentguild.internal.TencentChannelImpl
import love.forte.simbot.component.tencentguild.internal.TencentChannelImpl.Companion.tencentChannelImpl
import love.forte.simbot.component.tencentguild.internal.TencentGuildComponentBotImpl
import love.forte.simbot.component.tencentguild.internal.TencentGuildImpl
import love.forte.simbot.component.tencentguild.util.requestBy
import love.forte.simbot.literal
import love.forte.simbot.tencentguild.TencentChannelInfo
import love.forte.simbot.tencentguild.api.channel.GetChannelApi
import love.forte.simbot.tencentguild.api.channel.GetGuildChannelListApi
import love.forte.simbot.tencentguild.isGrouping
import love.forte.simbot.utils.item.Items
import love.forte.simbot.utils.item.Items.Companion.asItems
import love.forte.simbot.utils.item.effectedFlowItems
import love.forte.simbot.utils.runInBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal sealed class InternalTcgChannelContainer {
    abstract suspend fun get(id: String): TencentChannelImpl?
    abstract fun getBlocking(id: String): TencentChannelImpl?
    abstract suspend fun size(): Int
    abstract val size: Int
    abstract val values: Items<TencentChannelImpl>
}

internal inline val InternalTcgChannelContainer.memory: MemoryInternalTcgChannelContainer? get() = this as? MemoryInternalTcgChannelContainer

/**
 *
 * @author ForteScarlet
 */
internal class MemoryInternalTcgChannelContainer(
    val guild: TencentGuildImpl,
    val container: ConcurrentMap<String, TencentChannelImpl> = ConcurrentHashMap(),
) : InternalTcgChannelContainer() {
    
    override suspend fun get(id: String): TencentChannelImpl? {
        return container[id]
    }
    
    override fun getBlocking(id: String): TencentChannelImpl? {
        return container[id]
    }
    
    fun remove(id: String): TencentChannelImpl? {
        return container.remove(id)
    }
    
    override val size: Int
        get() = container.size
    
    override suspend fun size(): Int = size
    
    
    override val values: Items<TencentChannelImpl>
        get() = container.values.asItems()
    
    suspend inline fun computeAndGet(
        bot: TencentGuildComponentBotImpl,
        info: TencentChannelInfo,
    ): TencentChannelImpl {
        return computeAndGet(bot, info) {
            guild.channelCategoryContainer.get(info.parentId)
                ?: throw NoSuchElementException("Category(id=${info.parentId}, name=${info.name})")
        }
    }
    
    
    suspend inline fun computeAndGet(
        bot: TencentGuildComponentBotImpl,
        info: TencentChannelInfo,
        crossinline categoryCalculator: suspend () -> TencentChannelCategoryImpl,
    ): TencentChannelImpl {
        val category = categoryCalculator()
        return container.compute(info.id.literal) { _, v ->
            v?.also {
                it.source = info
            } ?: tencentChannelImpl(bot, info, guild, category)
        }!!
    }
    
    
}


internal class ApiInternalTcgChannelContainer(
    val guild: TencentGuildImpl,
    cacheMaxSize: Int = 100,
    onRemove: (TencentChannelImpl) -> Unit = {},
) : InternalTcgChannelContainer() {
    private val lock = Mutex()
    private val cache = createLruCacheMap<String, TencentChannelImpl>(cacheMaxSize, onRemove)
    
    private suspend fun getSync(id: String): TencentChannelImpl = lock.withLock {
        cache[id] ?: GetChannelApi(id.ID).requestBy(guild.baseBot).toImpl().also {
            cache[id] = it
        }
    }
    
    override suspend fun get(id: String): TencentChannelImpl {
        return cache[id] ?: getSync(id)
    }
    
    override fun getBlocking(id: String): TencentChannelImpl {
        return cache[id] ?: runInBlocking { getSync(id) }
    }
    
    override suspend fun size(): Int {
        var counter = 1
        allChannelImpls().collect {
            counter++
        }
        return counter
    }
    
    override val size: Int
        get() = runInBlocking { size() }
    
    
    override val values: Items<TencentChannelImpl>
        get() = allChannelImpls()
    
    private suspend fun TencentChannelInfo.toImpl(): TencentChannelImpl {
        val category = guild.channelCategoryContainer.get(parentId)
            ?: throw NoSuchElementException("Category(id=${parentId}) for channel(id=${id}, name=${name})")
        return tencentChannelImpl(guild.baseBot, this, guild, category)
    }
    
    private fun allChannelImpls(): Items<TencentChannelImpl> = guild.baseBot.effectedFlowItems {
        val list = GetGuildChannelListApi(guild.id).requestBy(guild.baseBot)
        list.forEach {
            if (!it.channelType.isGrouping) {
                emit(it.toImpl())
            }
        }
    }
}