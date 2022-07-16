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

import io.ktor.http.*
import love.forte.simbot.ID
import love.forte.simbot.LoggerFactory
import love.forte.simbot.Timestamp
import love.forte.simbot.component.tencentguild.TencentChannel
import love.forte.simbot.component.tencentguild.TencentGuild
import love.forte.simbot.component.tencentguild.TencentGuildComponentGuildBot
import love.forte.simbot.component.tencentguild.internal.container.*
import love.forte.simbot.component.tencentguild.internal.info.toInternal
import love.forte.simbot.component.tencentguild.util.requestBy
import love.forte.simbot.literal
import love.forte.simbot.tencentguild.EventSignals
import love.forte.simbot.tencentguild.TencentApiException
import love.forte.simbot.tencentguild.TencentGuildInfo
import love.forte.simbot.tencentguild.api.channel.GetGuildChannelListApi
import love.forte.simbot.tencentguild.api.member.GetMemberApi
import love.forte.simbot.tencentguild.api.role.GetGuildRoleListApi
import love.forte.simbot.tencentguild.isGrouping
import love.forte.simbot.utils.item.Items
import love.forte.simbot.utils.item.effectedFlowItems
import kotlin.time.Duration


/**
 *
 * @author ForteScarlet
 */
internal class TencentGuildImpl private constructor(
    internal val baseBot: TencentGuildComponentBotImpl,
    @Volatile override var source: TencentGuildInfo,
) : TencentGuild {
    private val logger =
        LoggerFactory.getLogger("love.forte.simbot.component.tencentguild.internal.TencentGuildImpl[${source.id}]")
    
    
    override val maximumChannel: Int
        get() = source.maximumChannel
    override val createTime: Timestamp
        get() = source.createTime
    override val currentMember: Int
        get() = source.currentMember
    override val description: String
        get() = source.description
    override val icon: String
        get() = source.icon
    override val id: ID
        get() = source.id
    override val maximumMember: Int
        get() = source.maximumMember
    override val name: String
        get() = source.name
    override val ownerId: ID
        get() = source.ownerId
    
    
    internal lateinit var channelCategoryContainer: InternalTcgChannelCategoryContainer
        private set
    internal lateinit var channelContainer: InternalTcgChannelContainer
        private set
    
    override lateinit var bot: TencentGuildComponentGuildBot
        internal set
    
    override lateinit var owner: TencentMemberImpl
        internal set
    
    override fun toString(): String {
        return "TencentGuildImpl(bot=$baseBot, guildInfo=$source)"
    }
    
    /**
     * 同步数据，包括成员信息和频道列表信息, 以及 [bot] 和 [owner] 的初始化。
     * 必须在对象实例后执行一次进行初始化，否则内部属性信息将会为空。
     *
     * *Note: 成员列表的获取暂时不支持（只支持私域）*
     */
    internal suspend fun initData() {
        // TODO 如果不支持，变更策略(查询时)
        syncData()
    }
    
    
    /**
     * 同步数据，包括成员信息和频道列表信息, 以及 [bot] 和 [owner] 的初始化。
     * 必须在对象实例后执行一次进行初始化，否则内部属性信息将会为空。
     *
     * *Note: 成员列表的获取暂时不支持（只支持私域）*
     */
    internal suspend fun syncData() {
        kotlin.runCatching { syncMembers() }.onFailure { e ->
            logger.error("Sync members data for guild $this failed.", e)
        }
        // TODO 如果不支持，变更策略(查询时)
        kotlin.runCatching { syncChannels() }.onFailure { e ->
            logger.error("Sync channels data for guild $this failed.", e)
        }
    }
    
    
    override suspend fun member(id: ID): TencentMemberImpl? {
        val member = kotlin.runCatching { GetMemberApi(source.id, id).requestBy(baseBot) }.getOrElse { e ->
            if (e is TencentApiException && e.value == 404) return@getOrElse null
            
            throw e
        }
        
        return member?.let { info -> TencentMemberImpl(baseBot, info.toInternal(), this) }
    }
    
    override val roles: Items<TencentRoleImpl>
        get() = bot.effectedFlowItems {
            GetGuildRoleListApi(source.id).requestBy(baseBot).roles.forEach { info ->
                val roleImpl = TencentRoleImpl(baseBot, info)
                emit(roleImpl)
            }
        }
    
    
    override val currentChannel: Int get() = channelContainer.size
    
    override val channels: Items<TencentChannelImpl>
        get() = channelContainer.values
    
    override suspend fun channel(id: ID): TencentChannel? {
        return channelContainer.get(id.literal)
    }
    
    override fun getChannel(id: ID): TencentChannel? {
        return channelContainer.getBlocking(id.literal)
    }
    
    override suspend fun mute(duration: Duration): Boolean = false
    
    /**
     * 同步成员列表、owner、bot的信息。
     */
    private suspend fun syncMembers() {
        // emm... 暂时不支持成员列表的获取. 反正挺神奇的
        
        syncBot()
        syncOwner()
    }
    
    private suspend fun syncOwner() {
        val ownerId = source.ownerId
        val ownerInfo = GetMemberApi(source.id, ownerId).requestBy(baseBot)
        
        owner = TencentMemberImpl(baseBot, ownerInfo, this)
        logger.debug("Sync guild owner: {}", ownerInfo)
        
    }
    
    private suspend fun syncBot() {
        val member = member(baseBot.source.botInfo.id)!!
        bot = TencentGuildComponentGuildBotImpl(baseBot, member)
        logger.debug("Sync guild bot: {}", bot)
    }
    
    
    private suspend fun syncChannels() {
        val channelModifyEventSignals = EventSignals.GuildMembers.intents
        val eventSupport = baseBot.source.clients.all {
            channelModifyEventSignals in it.intents
        }
        
        fun useApiContainer() {
            channelContainer = ApiInternalTcgChannelContainer(this)
            channelCategoryContainer = ApiInternalTcgChannelCategoryContainer(this)
        }
        
        if (!eventSupport) {
            logger.warn(
                "It does not support channel change events(Intents=EventSignals.GuildMembers.intents(value={})). Guild's channel api will directly use the API request mode.",
                EventSignals.GuildMembers.intents
            )
            useApiContainer()
            return
        }
        
        val channelInfoList = kotlin.runCatching {
            // group first
            GetGuildChannelListApi(source.id).requestBy(baseBot).sortedBy {
                if (it.channelType.isGrouping) 0 else 1
            }
        }.getOrElse {
            if (it is TencentApiException) {
                // 401权限不足,
                // 列表权限错误: 11264?
                if (it.value == HttpStatusCode.Unauthorized.value) {
                    if (logger.isDebugEnabled) {
                        logger.warn(
                            "Failed to get channel list for Guild(id={}, name={}). Will use API request mode. reason: TencentApiException: {}",
                            id,
                            name,
                            it.localizedMessage,
                            it
                        )
                    } else {
                        logger.warn(
                            "Failed to get channel list for Guild(id={}, name={}). Will use API request mode. reason: TencentApiException: {}",
                            id,
                            name,
                            it.localizedMessage
                        )
                    }
                    useApiContainer()
                }
            }
            throw it
        }
        
        logger.debug(
            "Sync the channel list for guild(id={}, name={}), {} pieces of synchronized data",
            id,
            name,
            channelInfoList.size
        )
        val channelContainer = MemoryInternalTcgChannelContainer(this)
        val categoryContainer = MemoryInternalTcgChannelCategoryContainer(this)
        
        this.channelContainer = channelContainer
        this.channelCategoryContainer = categoryContainer
        
        for (info in channelInfoList) {
            if (info.channelType.isGrouping) {
                categoryContainer.computeAndGet(baseBot, info)
            } else {
                channelContainer.computeAndGet(baseBot, info)
            }
        }
    }
    
    companion object {
        
        suspend fun tencentGuildImpl(
            baseBot: TencentGuildComponentBotImpl,
            guildInfo: TencentGuildInfo,
        ): TencentGuildImpl {
            return TencentGuildImpl(baseBot, guildInfo).also { it.initData() }
        }
    }
    
}



