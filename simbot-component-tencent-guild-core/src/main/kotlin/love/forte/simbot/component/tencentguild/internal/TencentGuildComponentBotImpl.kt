/*
 *  Copyright (c) 2021-2022 ForteScarlet <ForteScarlet@163.com>
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

import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import love.forte.simbot.Api4J
import love.forte.simbot.ID
import love.forte.simbot.LoggerFactory
import love.forte.simbot.component.tencentguild.TencentGuild
import love.forte.simbot.component.tencentguild.TencentGuildBotManager
import love.forte.simbot.component.tencentguild.TencentGuildComponent
import love.forte.simbot.component.tencentguild.TencentGuildComponentBot
import love.forte.simbot.component.tencentguild.event.TcgBotStartedEvent
import love.forte.simbot.component.tencentguild.internal.container.ApiInternalTcgGuildContainer.Companion.apiInternalTcgGuildContainer
import love.forte.simbot.component.tencentguild.internal.container.InternalTcgGuildContainer
import love.forte.simbot.component.tencentguild.internal.container.MemoryInternalTcgGuildContainer
import love.forte.simbot.component.tencentguild.internal.container.guildListFlow
import love.forte.simbot.component.tencentguild.internal.event.TcgBotStartedEventImpl
import love.forte.simbot.event.EventProcessor
import love.forte.simbot.event.pushIfProcessable
import love.forte.simbot.literal
import love.forte.simbot.tencentguild.EventSignals
import love.forte.simbot.tencentguild.TencentGuildBot
import love.forte.simbot.utils.item.Items
import java.util.concurrent.atomic.LongAdder
import kotlin.coroutines.CoroutineContext

/**
 *
 * @author ForteScarlet
 */
internal class TencentGuildComponentBotImpl(
    override val source: TencentGuildBot,
    override val manager: TencentGuildBotManager,
    override val eventProcessor: EventProcessor,
    override val component: TencentGuildComponent,
    internal val configuration: TencentGuildComponentBotConfiguration,
) : TencentGuildComponentBot {
    
    override val coroutineContext: CoroutineContext
        get() = source.coroutineContext
    
    private val job
        get() = source.coroutineContext[Job]!!
    
    override val logger =
        LoggerFactory.getLogger("love.forte.simbot.component.tencentguild.bot.${source.ticket.appKey}")
    
    @Volatile
    private lateinit var meId: ID
    
    override fun isMe(id: ID): Boolean {
        if (id == this.id) return true
        if (::meId.isInitialized && meId == id) return true
        return false
    }
    
    internal lateinit var guildContainer: InternalTcgGuildContainer
        private set
    
    override val guilds: Items<TencentGuildImpl>
        get() = guildContainer.values
    
    
    override suspend fun guild(id: ID): TencentGuild? {
        return guildContainer.get(id.literal)
    }
    
    @Api4J
    override fun getGuild(id: ID): TencentGuild? = guildContainer.getBlocking(id.literal)
    
    /**
     * 启动当前bot。
     */
    override suspend fun start(): Boolean = source.start().also {
        // just set everytime.
        source.botInfo
        meId = source.me().id
        
        suspend fun pushStartedEvent() {
            eventProcessor.pushIfProcessable(TcgBotStartedEvent) {
                TcgBotStartedEventImpl(this)
            }
        }
        
        if (!it) {
            pushStartedEvent()
            return@also
        }
        
        initData()
        registerEventProcessor()
        pushStartedEvent()
    }
    
    
    override suspend fun join() {
        source.join()
    }
    
    
    override suspend fun cancel(reason: Throwable?): Boolean = source.cancel(reason)
    
    
    @Api4J
    override fun cancelBlocking(reason: Throwable?): Boolean {
        return runBlocking { cancel(reason) }
    }
    
    override val isStarted: Boolean
        get() = job.isCompleted || job.isActive
    
    
    override val isCancelled: Boolean
        get() = job.isCancelled
    
    
    /**
     * 初始化bot基本信息.
     */
    private suspend fun initData() {
        initGuildListData()
    }
    
    private suspend fun initGuildListData() {
        // 是否支持 Guild 事件
        val isGuildEventSupport = source.clients.all { EventSignals.Guilds.intents in it.intents }
        if (!isGuildEventSupport) {
            logger.warn(
                "It does not support guild change events(Intents=EventSignals.Guilds.intent(value={})). Guild's api will directly use the API request mode.",
                EventSignals.Guilds.intents
            )
            this.guildContainer = apiInternalTcgGuildContainer(this)
            return
        }
        
        val guildContainer = MemoryInternalTcgGuildContainer(this)
        this.guildContainer = guildContainer
        
        val initDataJob = SupervisorJob(this.coroutineContext[Job])
        val counter = LongAdder()
        guildListFlow { batch, lastId, list ->
            logger.debug(
                "Sync batch {} of the guild list, {} pieces of synchronized data, after id: {}",
                batch,
                list.size,
                lastId
            )
        }.collect { info ->
            launch(initDataJob) {
                guildContainer.computeAndGet(info)
                counter.increment()
            }
        }
        
        logger.info("{} pieces of guild information are synchronized", counter)
        logger.info("Begin to initialize guild information asynchronously...")
        
        // wait for sync jobs...
        initDataJob.children.forEach {
            it.join()
        }
        initDataJob.cancel()
    }
    
}

