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
import love.forte.simbot.Timestamp
import love.forte.simbot.definition.Guild
import love.forte.simbot.definition.Organization
import love.forte.simbot.tencentguild.TencentGuildInfo
import love.forte.simbot.utils.item.Items
import love.forte.simbot.utils.item.Items.Companion.emptyItems
import love.forte.simbot.utils.runInBlocking

/**
 *
 * @author ForteScarlet
 */
public interface TencentGuild : Guild, TencentGuildObjectiveContainer<TencentGuildInfo> {
    override val bot: TencentGuildComponentGuildBot
    override val createTime: Timestamp
    override val currentMember: Int
    override val description: String
    override val icon: String
    override val id: ID
    override val maximumMember: Int
    override val name: String
    override val ownerId: ID
    
    /**
     * 得到此频道服务器下的所有子频道。
     */
    override val channels: Items<TencentChannel>
    
    
    /**
     * 获取指定ID的子频道。
     */
    @OptIn(Api4J::class)
    override fun getChannel(id: ID): TencentChannel?
    
    /**
     * 获取指定ID的子频道。
     */
    @JvmSynthetic
    override suspend fun channel(id: ID): TencentChannel?
    
    
    /**
     * 得到此频道服务器下的所有子频道。
     *
     * @see channels
     */
    override val children: Items<TencentChannel> get() = channels
    
    
    /**
     * 获取指定ID的子频道。
     *
     * @see channel
     */
    @JvmSynthetic
    override suspend fun child(id: ID): TencentChannel? = channel(id)
    
    
    /**
     * 获取指定ID的子频道。
     *
     * @see getChannel
     */
    @OptIn(Api4J::class)
    override fun getChild(id: ID): TencentChannel? = getChannel(id)
    
    
    @JvmSynthetic
    override suspend fun owner(): TencentMember = owner
    
    @OptIn(Api4J::class)
    override val owner: TencentMember
    
    /**
     * 获取指定成员信息。
     */
    @JvmSynthetic
    override suspend fun member(id: ID): TencentMember?
    
    /**
     * 获取指定成员信息。
     */
    @Api4J
    override fun getMember(id: ID): TencentMember? = runInBlocking { member(id) }
    
    /**
     * 频道无法获取成员列表。
     */
    @Deprecated(
        "Get member list is not supported",
        ReplaceWith("emptyItems()", "love.forte.simbot.utils.item.Items.Companion.emptyItems")
    )
    override val members: Items<TencentMember>
        get() = emptyItems()
    
    
    override val roles: Items<TencentRole>
    
    //// Impls
    
    @JvmSynthetic
    override suspend fun unmute(): Boolean = false
    
    @JvmSynthetic
    override suspend fun previous(): Organization? = null
    
    
    @OptIn(Api4J::class)
    override val previous: Organization?
        get() = null
}