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
import love.forte.simbot.component.tencentguild.TencentMember
import love.forte.simbot.component.tencentguild.event.TcgChannelAtMessageEvent
import love.forte.simbot.component.tencentguild.internal.*
import love.forte.simbot.component.tencentguild.util.requestBy
import love.forte.simbot.event.Event
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageReceipt
import love.forte.simbot.tencentguild.EventSignals
import love.forte.simbot.tencentguild.TencentMessage
import love.forte.simbot.tencentguild.api.channel.GetChannelApi
import love.forte.simbot.tencentguild.api.guild.GetGuildApi
import love.forte.simbot.tencentguild.api.message.MessageSendApi
import love.forte.simbot.utils.LazyValue
import love.forte.simbot.utils.lazyValue

/**
 *
 * @author ForteScarlet
 */
internal class TcgChannelAtMessageEventImpl(
    override val sourceEventEntity: TencentMessage,
    override val bot: TencentGuildComponentBotImpl,
) : TcgChannelAtMessageEvent() {
    override val id: ID = sourceEventEntity.id
    override suspend fun reply(message: Message): MessageReceipt {
        val messageForSend = MessageParsers.parse(message)
        messageForSend.msgId = sourceEventEntity.id
        val cid = sourceEventEntity.channelId
        return MessageSendApi(cid, messageForSend).requestBy(bot).asReceipt()
    }
    
    override suspend fun send(message: Message): MessageReceipt = reply(message)
    
    private val _fromGuild: LazyValue<TencentGuildImpl> = bot.lazyValue {
        val guildInfo = GetGuildApi(sourceEventEntity.guildId).requestBy(bot)
        TencentGuildImpl(baseBot = bot, guildInfo)
    }
    
    override suspend fun author(): TencentMember = _author.await()
    
    private val _author: LazyValue<TencentMemberImpl> = bot.lazyValue {
        TencentMemberImpl(
            bot = bot,
            info = sourceEventEntity.member,
            guild = _fromGuild.await()
        )
    }
    
    private var _sourceChannel: LazyValue<TencentChannelImpl> = bot.lazyValue {
        TencentChannelImpl(
            bot = bot,
            info = GetChannelApi(sourceEventEntity.channelId).requestBy(bot),
            from = _fromGuild.await()
        )
    }
    
    override suspend fun source(): TencentChannelImpl = _sourceChannel.await()
    override suspend fun channel(): TencentChannelImpl = _sourceChannel.await()
    
    override val messageContent: TencentReceiveMessageContentImpl by lazy(LazyThreadSafetyMode.NONE) {
        TencentReceiveMessageContentImpl(sourceEventEntity)
    }
    
    
    internal object Parser : BaseSignalToEvent<TencentMessage>() {
        override val key = Key
        override val type: EventSignals<TencentMessage>
            get() = EventSignals.AtMessages.AtMessageCreate
        
        override suspend fun doParser(data: TencentMessage, bot: TencentGuildComponentBotImpl): Event {
            return TcgChannelAtMessageEventImpl(data, bot)
        }
        
    }
}



