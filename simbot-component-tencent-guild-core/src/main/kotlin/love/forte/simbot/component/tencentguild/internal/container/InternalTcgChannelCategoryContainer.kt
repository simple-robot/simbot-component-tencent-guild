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

import love.forte.simbot.ID
import love.forte.simbot.SimbotIllegalStateException
import love.forte.simbot.component.tencentguild.internal.TencentChannelCategoryImpl
import love.forte.simbot.component.tencentguild.internal.TencentGuildComponentBotImpl
import love.forte.simbot.component.tencentguild.internal.TencentGuildImpl
import love.forte.simbot.component.tencentguild.util.requestBy
import love.forte.simbot.literal
import love.forte.simbot.tencentguild.TencentChannelInfo
import love.forte.simbot.tencentguild.api.channel.GetChannelApi
import love.forte.simbot.tencentguild.isGrouping
import love.forte.simbot.utils.runInBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal sealed class InternalTcgChannelCategoryContainer {
    abstract suspend fun get(id: String): TencentChannelCategoryImpl?
    abstract fun getBlocking(id: String): TencentChannelCategoryImpl?
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


internal class ApiInternalTcgChannelCategoryContainer(val guild: TencentGuildImpl) :
    InternalTcgChannelCategoryContainer() {
    override suspend fun get(id: String): TencentChannelCategoryImpl {
        val channelInfo = GetChannelApi(id.ID).requestBy(guild.baseBot)
        if (!channelInfo.channelType.isGrouping) {
            throw SimbotIllegalStateException("Channel(id=$id) is not a category channel.")
        }
        
        return TencentChannelCategoryImpl(guild.baseBot, guild, channelInfo)
    }
    
    override fun getBlocking(id: String): TencentChannelCategoryImpl {
        return runInBlocking { get(id) }
    }
    
}