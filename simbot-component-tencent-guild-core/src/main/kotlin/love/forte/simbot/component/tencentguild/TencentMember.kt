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

package love.forte.simbot.component.tencentguild

import love.forte.simbot.Api4J
import love.forte.simbot.ID
import love.forte.simbot.bot.Bot
import love.forte.simbot.component.tencentguild.internal.TencentMessageReceipt
import love.forte.simbot.definition.GuildMember
import love.forte.simbot.definition.MemberInfo
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageContent
import love.forte.simbot.tencentguild.TencentMemberInfo
import love.forte.simbot.utils.item.Items
import love.forte.simbot.utils.runInBlocking
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 *
 * @author ForteScarlet
 */
public interface TencentMember : GuildMember, MemberInfo, TencentGuildObjectiveContainer<TencentMemberInfo> {
    override val avatar: String
    override val bot: Bot
    override val id: ID
    
    override val username: String
    
    override val roles: Items<TencentRole>
    
    
    @OptIn(Api4J::class)
    override val guild: TencentGuild
    
    override suspend fun send(message: Message): TencentMessageReceipt
    override suspend fun send(text: String): TencentMessageReceipt
    override suspend fun send(message: MessageContent): TencentMessageReceipt
    
    @Api4J
    override fun sendBlocking(text: String): TencentMessageReceipt = runInBlocking { send(text) }
    
    @Api4J
    override fun sendBlocking(message: Message): TencentMessageReceipt = runInBlocking { send(message) }
    
    @Api4J
    override fun sendBlocking(message: MessageContent): TencentMessageReceipt = runInBlocking { send(message) }
    
    //// Impl
    
    @JvmSynthetic
    override suspend fun organization(): TencentGuild = guild
    
    @JvmSynthetic
    override suspend fun guild(): TencentGuild = guild
    
    
    @Deprecated("子频道不支持禁言", ReplaceWith("false"))
    @JvmSynthetic
    override suspend fun mute(duration: Duration): Boolean = false
    
    @Deprecated("子频道不支持禁言", ReplaceWith("false"))
    @JvmSynthetic
    override suspend fun unmute(): Boolean = false
    
    @Deprecated("子频道不支持禁言", ReplaceWith("false"))
    @OptIn(Api4J::class)
    override fun muteBlocking(time: Long, timeUnit: TimeUnit): Boolean = false
    
    @OptIn(Api4J::class)
    @Deprecated("子频道不支持禁言", ReplaceWith("false"))
    override fun unmuteBlocking(): Boolean = false
    
    
    @OptIn(Api4J::class)
    override val organization: TencentGuild get() = guild
    
    
}