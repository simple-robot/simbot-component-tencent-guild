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

import love.forte.simbot.ID
import love.forte.simbot.component.tencentguild.TencentChannelCategory
import love.forte.simbot.component.tencentguild.TencentGuildComponentGuildBot
import love.forte.simbot.tencentguild.ChannelSubType
import love.forte.simbot.tencentguild.ChannelType
import love.forte.simbot.tencentguild.TencentChannelInfo

/**
 *
 * @author ForteScarlet
 */
internal class TencentChannelCategoryImpl(
    private val baseBot: TencentGuildComponentBotImpl,
    override val guild: TencentGuildImpl,
    @Volatile internal var source: TencentChannelInfo,
) : TencentChannelCategory {
    override val bot: TencentGuildComponentGuildBot
        get() = guild.bot
    override val id: ID
        get() = source.id
    override val name: String
        get() = source.name
    override val guildId: ID
        get() = source.guildId
    override val channelType: ChannelType
        get() = source.channelType
    override val channelSubType: ChannelSubType
        get() = source.channelSubType
    override val position: Int
        get() = source.position
    override val parentId: String
        get() = source.parentId
    override val ownerId: ID
        get() = source.ownerId
}