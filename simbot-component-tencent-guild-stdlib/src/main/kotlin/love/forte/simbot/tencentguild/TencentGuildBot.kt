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

package love.forte.simbot.tencentguild

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import love.forte.simbot.Api4J
import love.forte.simbot.tencentguild.api.user.GetBotInfoApi
import kotlin.coroutines.CoroutineContext


/**
 *
 * 一个 [TencentGuildBot] 标准接口，其只提供接口定义，而实现则在 `core` 模块中。
 *
 * [TencentGuildBot] 不考虑实现 simple-robot-api 中的 [love.forte.simbot.Bot] 接口，由对组件进行实现，此处的 [TencentGuildBot] 仅定义对于一个频道机器人的最基本的信息。
 *
 * @author ForteScarlet
 */
public interface TencentGuildBot : CoroutineScope {
    override val coroutineContext: CoroutineContext
    
    /**
     * 当前bot的 [Ticket].
     */
    public val ticket: Ticket
    
    /**
     * Bot当前的配置信息。
     */
    public val configuration: TencentGuildBotConfiguration
    
    /**
     * Bot自己的信息。
     * 一般来讲，只有当bot启动后才能获取到此属性。
     */
    public val botInfo: TencentBotInfo
    
    /**
     * 添加一个事件预处理器。
     * [processor] 中的参数 `decoded` 所代表的为 `decoder.decodeFromJsonElement(eventType.decoder, data)` 后的结果，
     * 但是为了避免在多个事件处理器中频繁进行反序列化，因此提供这个 `decoded` 来预先提供一个懒实例化的获取器。
     *
     * 预处理器与 [processor] 不同的是，[preProcessor] 所注册的所有事件会在接收到事件的时候以协程**同步**的方式按照顺序执行，
     * 而后再交由下游的 [processor] 处理。
     *
     */
    @JvmSynthetic
    public fun preProcessor(processor: suspend Signal.Dispatch.(decoder: Json, decoded: () -> Any) -> Unit)
    
    /**
     * 添加一个事件处理器。
     * [processor] 中的参数 `decoded` 所代表的为 `decoder.decodeFromJsonElement(eventType.decoder, data)` 后的结果，
     * 但是为了避免在多个事件处理器中频繁进行反序列化，因此提供这个 `decoded` 来预先提供一个懒实例化的获取器。
     *
     * [processor] 注册的所有事件会在每次接收到事件的时候**异步**的按照顺序执行。
     *
     */
    @JvmSynthetic
    public fun processor(processor: suspend Signal.Dispatch.(decoder: Json, decoded: () -> Any) -> Unit)
    
    
    /**
     * process for java
     */
    @Api4J
    public fun process(processor: (Signal.Dispatch, Json, decoded: () -> Any) -> Unit) {
        processor { decoder, decoded -> processor(this, decoder, decoded) }
    }
    
    /**
     * process for java
     */
    @Api4J
    public fun <R : Any> process(eventType: EventSignals<R>, processor: (R) -> Unit) {
        processor { _, decoded ->
            if (type == eventType.type) {
                // val eventData: R = decoder.decodeFromJsonElement(eventType.decoder, data)
                @Suppress("UNCHECKED_CAST")
                processor(decoded() as R)
            }
        }
    }
    
    /**
     * [票据](https://bot.q.qq.com/wiki/develop/api/#%E7%A5%A8%E6%8D%AE)
     */
    public interface Ticket {
        /**
         * 用于识别一个机器人的 id
         */
        public val appId: String
        
        /**
         * 用于在 oauth 场景进行请求签名的密钥，在一些描述中也叫做 app_secret
         */
        public val appKey: String
        
        /**
         * 机器人token，用于以机器人身份调用 openapi，格式为 ${app_id}.${random_str}
         */
        public val token: String
        
        /**
         * 拼接bot token
         */
        public val botToken: String
    }
    
    
    /**
     * 启动当前BOT。只有 [start] 了之后，[clients] 中才会出现数据，否则可能会抛出异常。
     *
     * @return 当且仅当启动了并且成功了才会得到true。
     */
    @JvmSynthetic
    public suspend fun start(): Boolean
    
    
    @Api4J
    public fun startBlocking(): Boolean = runBlocking { start() }
    
    
    /**
     * 终止当前BOT。
     *
     * @return 当且仅当此BOT未关闭且关闭成功才会得到true。
     */
    @JvmSynthetic
    public suspend fun cancel(reason: Throwable? = null): Boolean
    
    @Api4J
    public fun cancelBlocking(reason: Throwable?): Boolean = runBlocking { cancel(reason) }
    
    /**
     * 挂起直到此bot被 [cancel]. 如果已经 [cancel], 则不会挂起。
     */
    @JvmSynthetic
    public suspend fun join()
    
    
    @Api4J
    public fun joinBlocking(): Unit = runBlocking { join() }
    
    
    /**
     * 此bot所需要的全部分片数量。
     * 这并不代表此bot中所**包含**的分片数量，而是代表你通过 /gateway/bot 或者根据你自己的判断而决定的最终的所有分片数量。
     *
     * 如果你想知道当前bot中包含了那几个分片，启动后通过 [clients] 中的 [shared][Client.shard] 可以得到。
     *
     */
    public val totalShared: Int
    
    
    /**
     * 此Bot中包含的连接。
     *
     * 如果此一个bot存在多个分片，那么这可能包
     *
     */
    public val clients: List<Client>
    
    
    /**
     * Bot的连接信息。一般来讲代表一个ws连接。
     */
    public interface Client {
        
        /**
         * 此连接所属的 [TencentGuildBot]
         */
        public val bot: TencentGuildBot
        
        /**
         * 此连接对应的分片信息。
         */
        public val shard: Shard
        
        /**
         * 事件推送所携带的 `s`.
         */
        public val seq: Long
        
        /**
         * 当前 client 是否处于运行状态。
         */
        public val isActive: Boolean
        
        /**
         * 是否正处于重新连接状态.
         */
        public val isResuming: Boolean
        
    }
    
    
    //// some self api
    /**
     * 通过 api [GetBotInfoApi] 得到bot自身信息。
     */
    public suspend fun me(): TencentBotInfo
    
    
}