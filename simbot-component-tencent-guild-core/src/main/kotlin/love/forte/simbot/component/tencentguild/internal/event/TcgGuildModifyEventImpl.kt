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

package love.forte.simbot.component.tencentguild.internal.event

import love.forte.simbot.ID
import love.forte.simbot.Timestamp
import love.forte.simbot.component.tencentguild.event.TcgGuildModifyEvent
import love.forte.simbot.component.tencentguild.internal.TencentGuildComponentBotImpl
import love.forte.simbot.component.tencentguild.internal.TencentGuildImpl
import love.forte.simbot.component.tencentguild.internal.container.ApiInternalTcgGuildContainer
import love.forte.simbot.component.tencentguild.internal.container.MemoryInternalTcgGuildContainer
import love.forte.simbot.literal
import love.forte.simbot.tencentguild.EventSignals
import love.forte.simbot.tencentguild.TencentGuildInfo

private suspend fun TencentGuildComponentBotImpl.findOrCreateGuild(data: TencentGuildInfo): TencentGuildImpl {
    return when (val container = guildContainer) {
        is MemoryInternalTcgGuildContainer -> container.computeAndGet(data)
        is ApiInternalTcgGuildContainer -> container.get(data.id.literal)
    }
}

internal class TcgGuildCreate(
    override val sourceEventEntity: TencentGuildInfo,
    override val bot: TencentGuildComponentBotImpl,
    override val guild: TencentGuildImpl,
) : TcgGuildModifyEvent.Create() {
    override val timestamp: Timestamp get() = changedTime
    override val id: ID = tcgGuildModifyId(0, bot.id, guild.id, timestamp)
    
    internal object Parser : BaseSignalToEvent<TencentGuildInfo>() {
        override val key = Create
        override val type: EventSignals<TencentGuildInfo>
            get() = EventSignals.Guilds.GuildCreate
        
        override suspend fun doParser(data: TencentGuildInfo, bot: TencentGuildComponentBotImpl): TcgGuildCreate {
            val guild = bot.findOrCreateGuild(data)
            return TcgGuildCreate(data, bot, guild)
        }
    }
}


internal class TcgGuildUpdate(
    override val sourceEventEntity: TencentGuildInfo,
    override val bot: TencentGuildComponentBotImpl,
    override val guild: TencentGuildImpl,
) : TcgGuildModifyEvent.Update() {
    override val id: ID = tcgGuildModifyId(1, bot.id, guild.id, timestamp)
    override val timestamp: Timestamp get() = changedTime
    
    internal object Parser : BaseSignalToEvent<TencentGuildInfo>() {
        override val key = Update
        override val type: EventSignals<TencentGuildInfo>
            get() = EventSignals.Guilds.GuildUpdate
        
        override suspend fun doParser(data: TencentGuildInfo, bot: TencentGuildComponentBotImpl): TcgGuildUpdate {
            val guild = bot.findOrCreateGuild(data)
            return TcgGuildUpdate(data, bot, guild)
        }
    }
}


internal class TcgGuildDelete(
    override val sourceEventEntity: TencentGuildInfo,
    override val bot: TencentGuildComponentBotImpl,
    override val guild: TencentGuildImpl,
) : TcgGuildModifyEvent.Delete() {
    override val timestamp: Timestamp
        get() = changedTime
    override val id: ID = tcgGuildModifyId(2, bot.id, guild.id, timestamp)
    
    
    internal object Parser : BaseSignalToEvent<TencentGuildInfo>() {
        override val key = Delete
        override val type: EventSignals<TencentGuildInfo>
            get() = EventSignals.Guilds.GuildDelete
        
        override suspend fun doParser(data: TencentGuildInfo, bot: TencentGuildComponentBotImpl): TcgGuildDelete {
            val guild = bot.findOrCreateGuild(data)
            return TcgGuildDelete(data, bot, guild)
        }
    }
}


private fun tcgGuildModifyId(t: Int, sourceBot: ID, sourceGuild: ID, timestamp: Timestamp): ID =
    "$t$sourceBot.${timestamp.second}.$sourceGuild".ID
