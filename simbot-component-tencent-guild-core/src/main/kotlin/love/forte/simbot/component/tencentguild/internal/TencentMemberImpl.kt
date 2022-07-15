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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import love.forte.simbot.ID
import love.forte.simbot.Timestamp
import love.forte.simbot.component.tencentguild.TencentMember
import love.forte.simbot.component.tencentguild.TencentRole
import love.forte.simbot.component.tencentguild.event.TcgChannelAtMessageEvent
import love.forte.simbot.component.tencentguild.util.requestBy
import love.forte.simbot.event.EventProcessingContext
import love.forte.simbot.literal
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageContent
import love.forte.simbot.tencentguild.DirectMessageSession
import love.forte.simbot.tencentguild.TencentMemberInfo
import love.forte.simbot.tencentguild.api.message.direct.CreateDmsApi
import love.forte.simbot.tencentguild.api.message.direct.DmsSendApi
import love.forte.simbot.utils.item.Items
import love.forte.simbot.utils.item.effectedItemsByFlow

/**
 *
 * @author ForteScarlet
 */
internal class TencentMemberImpl(
    override val bot: TencentGuildComponentBotImpl,
    @Volatile
    override var source: TencentMemberInfo,
    override val guild: TencentGuildImpl,
) : TencentMember {
    override val joinTime: Timestamp
        get() = source.joinTime
    override val nickname: String
        get() = source.nickname
    override val avatar: String
        get() = source.avatar
    override val id: ID
        get() = source.id
    override val username: String
        get() = source.username
    
    private val roleIdSet = source.roleIds.mapTo(mutableSetOf()) { it.literal }
    private lateinit var dms: DirectMessageSession
    private val dmsInitLock = Mutex()
    private suspend fun getDms(): DirectMessageSession {
        if (::dms.isInitialized) {
            return dms
        } else {
            dmsInitLock.withLock {
                if (::dms.isInitialized) {
                    return dms
                }
                return CreateDmsApi(id, guild.id).requestBy(bot).also {
                    dms = it
                }
            }
        }
    }
    
    override val roles: Items<TencentRole>
        get() {
            
            return bot.effectedItemsByFlow {
                guild.roles.asFlow().filter { it.id.literal in roleIdSet }
            }
            
        }
    
    
    @JvmSynthetic
    override suspend fun send(message: Message): TencentMessageReceipt {
        val dms = getDms()
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
        
        
        return DmsSendApi(dms.guildId, messageForSend).requestBy(bot).asReceipt()
    }
    
    override suspend fun send(text: String): TencentMessageReceipt {
        val dms = getDms()
        val currentEvent =
            currentCoroutineContext()[EventProcessingContext]?.event?.takeIf { it is TcgChannelAtMessageEvent } as? TcgChannelAtMessageEvent
        val msgId = currentEvent?.sourceEventEntity?.id
        
        return DmsSendApi(guildId = dms.guildId, content = text, msgId = msgId).requestBy(bot).asReceipt()
    }
    
    
    override suspend fun send(message: MessageContent): TencentMessageReceipt {
        return send(message.messages)
    }
    
    override fun toString(): String {
        return "TencentMemberImpl(bot=$bot, source=$source, guild=$guild)"
    }
}

