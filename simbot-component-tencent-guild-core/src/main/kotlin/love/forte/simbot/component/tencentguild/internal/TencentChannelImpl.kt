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

package love.forte.simbot.component.tencentguild.internal

import kotlinx.coroutines.currentCoroutineContext
import love.forte.simbot.ID
import love.forte.simbot.Timestamp
import love.forte.simbot.component.tencentguild.TencentChannel
import love.forte.simbot.component.tencentguild.TencentGuildComponentGuildBot
import love.forte.simbot.component.tencentguild.TencentMember
import love.forte.simbot.component.tencentguild.TencentRole
import love.forte.simbot.component.tencentguild.event.TcgChannelAtMessageEvent
import love.forte.simbot.component.tencentguild.util.requestBy
import love.forte.simbot.event.EventProcessingContext
import love.forte.simbot.message.Message
import love.forte.simbot.tencentguild.TencentChannelInfo
import love.forte.simbot.tencentguild.api.message.MessageSendApi
import love.forte.simbot.utils.item.Items

/**
 *
 * @author ForteScarlet
 */
internal class TencentChannelImpl private constructor(
    private val baseBot: TencentGuildComponentBotImpl,
    @Volatile
    override var source: TencentChannelInfo,
    override val guild: TencentGuildImpl,
    override val category: TencentChannelCategoryImpl,
) : TencentChannel {
    
    override val bot: TencentGuildComponentGuildBot get() = guild.bot
    
    override suspend fun send(message: Message): TencentMessageReceipt {
        val currentCoroutineContext = currentCoroutineContext()
        
        
        val messageForSend = MessageParsers.parse(message) {
            if (this.msgId == null) {
                val currentEvent =
                    currentCoroutineContext[EventProcessingContext]?.event?.takeIf { it is TcgChannelAtMessageEvent } as? TcgChannelAtMessageEvent
                
                val msgId = currentEvent?.sourceEventEntity?.id
                if (msgId != null) {
                    this.msgId = msgId
                }
            }
        }
        return MessageSendApi(source.id, messageForSend).requestBy(baseBot).asReceipt()
    }
    
    override val owner: TencentMember
        get() = guild.owner
    
    
    // TODO
    override val roles: Items<TencentRole>
        get() = guild.roles
    
    override fun toString(): String {
        return "TencentChannelImpl(bot=$baseBot, source=$source, guild=$guild)"
    }
    
    // region info impl
    @Suppress("DEPRECATION")
    override val createTime: Timestamp
        get() = source.createTime
    
    @Suppress("DEPRECATION")
    override val currentMember: Int
        get() = source.currentMember
    
    @Suppress("DEPRECATION")
    override val description: String
        get() = source.description
    override val guildId: ID
        get() = source.guildId
    
    override val icon: String
        get() = guild.icon
    
    override val id: ID
        get() = source.id
    
    @Suppress("DEPRECATION")
    override val maximumMember: Int
        get() = source.maximumMember
    
    @Suppress("DEPRECATION")
    override val name: String
        get() = source.name
    override val ownerId: ID
        get() = source.ownerId
    // endregion
    
    companion object {
        fun tencentChannelImpl(
            baseBot: TencentGuildComponentBotImpl,
            source: TencentChannelInfo,
            guild: TencentGuildImpl,
            category: TencentChannelCategoryImpl,
        ): TencentChannelImpl {
            return TencentChannelImpl(baseBot, source, guild, category)
        }
    }
}