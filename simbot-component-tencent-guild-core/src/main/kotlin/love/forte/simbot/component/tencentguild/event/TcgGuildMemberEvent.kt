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

package love.forte.simbot.component.tencentguild.event

import kotlinx.coroutines.runBlocking
import love.forte.simbot.Api4J
import love.forte.simbot.action.ActionType
import love.forte.simbot.component.tencentguild.TencentGuild
import love.forte.simbot.component.tencentguild.TencentMember
import love.forte.simbot.definition.MemberInfo
import love.forte.simbot.event.*
import love.forte.simbot.message.doSafeCast
import love.forte.simbot.tencentguild.EventSignals
import love.forte.simbot.tencentguild.TencentMemberInfo

/**
 *
 * 频道成员变更事件。
 *
 *
 *
 * @author ForteScarlet
 */
@BaseEvent
public sealed class TcgGuildMemberEvent : TcgEvent<TencentMemberInfo>(), GuildEvent, MemberEvent {
    abstract override val key: Event.Key<out TcgGuildMemberEvent>


    /**
     * 涉及的频道服务器信息。同 [organization].
     */
    @JvmSynthetic
    abstract override suspend fun guild(): TencentGuild

    /**
     * 涉及的用户信息。同 [user].
     */
    @JvmSynthetic
    abstract override suspend fun member(): TencentMember

    /**
     * 涉及的用户信息。同 [member].
     */
    @JvmSynthetic
    override suspend fun user(): TencentMember = member()

    /**
     * 涉及的频道服务器信息。同 [guild].
     */
    @JvmSynthetic
    override suspend fun organization(): TencentGuild = guild()

    /**
     * 涉及的频道服务器信息。同 [organization].
     */
    @Api4J
    override val guild: TencentGuild
        get() = runBlocking { guild() }

    /**
     * 涉及的用户信息。同 [user].
     */
    @Api4J
    override val member: TencentMember
        get() = runBlocking { member() }

    /**
     * 涉及的用户信息。同 [member].
     */
    @Api4J
    override val user: TencentMember
        get() = runBlocking { user() }

    /**
     * 涉及的频道服务器信息。同 [guild].
     */
    @Api4J
    override val organization: TencentGuild
        get() = runBlocking { organization() }


    public companion object Key : BaseEventKey<TcgGuildMemberEvent>(
        "tcg.guild_member", setOf(
            TcgEvent, MemberEvent, GuildEvent
        )
    ) {
        override fun safeCast(value: Any): TcgGuildMemberEvent? = doSafeCast(value)
    }

    /**
     * 成员增加事件，同时属于 [GuildEvent].
     *
     * 发送时机：新用户加入频道
     */
    public abstract class Increase : TcgGuildMemberEvent(), MemberIncreaseEvent, GuildEvent {
        /**
         * 增加的成员。同 [user].
         */
        @OptIn(Api4J::class)
        abstract override val member: TencentMember

        /**
         * 增加的成员。同 [user].
         */
        @JvmSynthetic
        override suspend fun member(): TencentMember = member

        /**
         * 增加的成员。同 [member].
         */
        @OptIn(Api4J::class)
        override val user: TencentMember get() = member

        /**
         * 增加的成员。同 [member].
         */
        @JvmSynthetic
        override suspend fun user(): TencentMember = member()

        /**
         * 增加的成员。同 [member].
         */
        @OptIn(Api4J::class)
        override val after: TencentMember get() = member

        /**
         * 增加的成员。同 [member].
         */
        @JvmSynthetic
        override suspend fun after(): TencentMember = member

        /**
         * 增加成员的频道。
         */
        @JvmSynthetic
        abstract override suspend fun source(): TencentGuild

        /**
         * 增加成员的频道。
         */
        @Api4J
        override val source: TencentGuild get() = runBlocking { source() }


        /**
         * 增加成员的频道。同 [source].
         */
        @Api4J
        override val organization: TencentGuild get() = source


        /**
         * 增加成员的频道。同 [source].
         */
        @JvmSynthetic
        override suspend fun organization(): TencentGuild = source()

        /**
         * 增加成员的频道。同 [source].
         */
        @Api4J
        override val guild: TencentGuild get() = source

        /**
         * 增加成员的频道。同 [source].
         */
        @JvmSynthetic
        override suspend fun guild(): TencentGuild = source()


        /**
         * 无法判断加入类型。通常为频道分享链接点击加入，视为主动加入。
         */
        override val actionType: ActionType
            get() = ActionType.PROACTIVE

        /**
         * 操作者。无法得知，始终为null。
         */
        @OptIn(Api4J::class)
        override val operator: MemberInfo?
            get() = null

        /**
         * 操作者。无法得知，始终为null。
         */
        @JvmSynthetic
        override suspend fun operator(): MemberInfo? = null


        override val key: Event.Key<Increase> get() = Key

        override val eventSignal: EventSignals<TencentMemberInfo>
            get() = EventSignals.GuildMembers.GuildMemberAdd


        public companion object Key : BaseEventKey<Increase>(
            "tcg.guild_member_increase", setOf(
                TcgGuildMemberEvent, MemberIncreaseEvent, GuildEvent
            )
        ) {
            override fun safeCast(value: Any): Increase? = doSafeCast(value)
        }

    }

    /**
     * 成员资料变更。无法得到变更前的信息。
     *
     * @see EventSignals.GuildMembers.GuildMemberUpdate
     */
    @Deprecated("暂无")
    public abstract class Update : TcgGuildMemberEvent(), MemberChangedEvent {
        // TODO
        
        @OptIn(Api4J::class)
        abstract override val after: TencentMember
        
        override suspend fun after(): TencentMember = after
        
        
        public companion object Key : BaseEventKey<Decrease>(
            "tcg.guild_member_update", setOf(
                TcgGuildMemberEvent, MemberChangedEvent
            )
        ) {
            override fun safeCast(value: Any): Decrease? = doSafeCast(value)
        }
    }

    /**
     * 成员减少：成员离开或退出，同时属于 [GuildEvent].
     *
     * 发送时机: 用户离开频道
     *
     */
    public abstract class Decrease : TcgGuildMemberEvent(),
        MemberDecreaseEvent, GuildEvent {

        /**
         * 离开的成员。同 [user].
         */
        @OptIn(Api4J::class)
        abstract override val member: TencentMember

        /**
         * 离开的成员。同 [user].
         */
        @JvmSynthetic
        override suspend fun member(): TencentMember = member

        /**
         * 离开的成员。同 [member].
         */
        @OptIn(Api4J::class)
        override val user: TencentMember get() = member

        /**
         * 离开的成员。同 [member].
         */
        @JvmSynthetic
        override suspend fun user(): TencentMember = member()

        /**
         * 离开的成员。同 [member].
         */
        @OptIn(Api4J::class)
        override val before: TencentMember get() = member

        /**
         * 离开的成员。同 [member].
         */
        @JvmSynthetic
        override suspend fun before(): TencentMember = member

        /**
         * 离开成员的频道。
         */
        @JvmSynthetic
        abstract override suspend fun source(): TencentGuild

        /**
         * 离开成员的频道。
         */
        @Api4J
        override val source: TencentGuild get() = runBlocking { source() }


        /**
         * 离开成员的频道。同 [source].
         */
        @Api4J
        override val organization: TencentGuild get() = source


        /**
         * 离开成员的频道。同 [source].
         */
        @JvmSynthetic
        override suspend fun organization(): TencentGuild = source()

        /**
         * 离开成员的频道。同 [source].
         */
        @Api4J
        override val guild: TencentGuild get() = source

        /**
         * 离开成员的频道。同 [source].
         */
        @JvmSynthetic
        override suspend fun guild(): TencentGuild = source()

        /**
         * 离开频道是主动的。（无法区分）
         */
        override val actionType: ActionType get() = ActionType.PROACTIVE

        /**
         * 无法获取操作者，通常为其自行操作。始终为null。
         */
        @JvmSynthetic
        override suspend fun operator(): MemberInfo? = null

        /**
         * 无法获取操作者，通常为其自行操作。始终为null。
         */
        @OptIn(Api4J::class)
        override val operator: MemberInfo?
            get() = null


        override val eventSignal: EventSignals<TencentMemberInfo>
            get() = EventSignals.GuildMembers.GuildMemberRemove

        override val key: Event.Key<Decrease> get() = Key

        public companion object Key : BaseEventKey<Decrease>(
            "tcg.guild_member_decrease", setOf(
                TcgGuildMemberEvent, MemberDecreaseEvent, GuildEvent
            )
        ) {
            override fun safeCast(value: Any): Decrease? = doSafeCast(value)
        }
    }


}