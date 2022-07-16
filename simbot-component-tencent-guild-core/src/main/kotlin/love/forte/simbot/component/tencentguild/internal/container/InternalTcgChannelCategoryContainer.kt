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
import love.forte.simbot.Simbot
import love.forte.simbot.SimbotIllegalStateException
import love.forte.simbot.component.tencentguild.internal.TencentChannelCategoryImpl
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

internal sealed class InternalTcgChannelCategoryContainer {
    abstract suspend fun get(id: String): TencentChannelCategoryImpl?
    abstract fun getBlocking(id: String): TencentChannelCategoryImpl?
    abstract suspend fun size(): Int
    abstract val size: Int
    abstract val values: Items<TencentChannelCategoryImpl>
}

/**
 *
 * @author ForteScarlet
 */
internal class MemoryInternalTcgChannelCategoryContainer(
    val guild: TencentGuildImpl,
    private val container: ConcurrentMap<String, TencentChannelCategoryImpl> = ConcurrentHashMap(),
) : InternalTcgChannelCategoryContainer() {
    
    override suspend fun get(id: String): TencentChannelCategoryImpl? {
        return container[id]
    }
    
    override fun getBlocking(id: String): TencentChannelCategoryImpl? {
        return container[id]
    }
    
    fun remove(id: String): TencentChannelCategoryImpl? {
        return container.remove(id)
    }
    
    override suspend fun size(): Int {
        return container.size
    }
    
    override val size: Int
        get() = container.size
    
    override val values: Items<TencentChannelCategoryImpl>
        get() = container.values.asItems()
    
    fun computeAndGet(
        bot: TencentGuildComponentBotImpl,
        info: TencentChannelInfo,
    ): TencentChannelCategoryImpl {
        return container.compute(info.id.literal) { _, v ->
            v?.also {
                it.source = info
            } ?: TencentChannelCategoryImpl(bot, guild, info)
        }!!
    }
}


internal class ApiInternalTcgChannelCategoryContainer(
    val guild: TencentGuildImpl,
    cacheMaxSize: Int = 100,
    onRemove: (TencentChannelCategoryImpl) -> Unit = {},
) : InternalTcgChannelCategoryContainer() {
    private val lock = Mutex()
    private val cache = createLruCacheMap<String, TencentChannelCategoryImpl>(cacheMaxSize, onRemove)
    
    private suspend fun getSync(id: String): TencentChannelCategoryImpl = lock.withLock {
        cache[id] ?: run {
            val channelInfo = GetChannelApi(id.ID).requestBy(guild.baseBot)
            if (!channelInfo.channelType.isGrouping) {
                throw SimbotIllegalStateException("Channel(id=$id) is not a category channel.")
            }
            
            channelInfo.toImpl().also {
                cache[id] = it
            }
        }
    }
    
    override suspend fun get(id: String): TencentChannelCategoryImpl {
        return cache[id] ?: getSync(id)
    }
    
    override fun getBlocking(id: String): TencentChannelCategoryImpl {
        return cache[id] ?: runInBlocking { getSync(id) }
    }
    
    override suspend fun size(): Int {
        var num = 0
        allCategoryImpls().collect {
            num++
        }
        return num
    }
    
    override val size: Int
        get() = runInBlocking { size() }
    
    override val values: Items<TencentChannelCategoryImpl>
        get() = allCategoryImpls()
    
    private suspend fun TencentChannelInfo.toImpl(): TencentChannelCategoryImpl {
        val info = GetChannelApi(id).requestBy(guild.baseBot)
        Simbot.check(info.channelType.isGrouping) { "Channel(id=${info.id}, name=${info.name}) is not a category channel." }
        
        return TencentChannelCategoryImpl(guild.baseBot, guild, info)
    }
    
    private fun allCategoryImpls(): Items<TencentChannelCategoryImpl> = guild.baseBot.effectedFlowItems {
        val list = GetGuildChannelListApi(guild.id).requestBy(guild.baseBot)
        list.forEach {
            // only category
            if (it.channelType.isGrouping) {
                emit(it.toImpl())
            }
        }
    }
}