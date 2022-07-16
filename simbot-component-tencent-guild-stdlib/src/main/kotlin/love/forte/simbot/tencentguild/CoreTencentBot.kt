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

@file:JvmName("TencentGuildBotFactory")

package love.forte.simbot.tencentguild

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import love.forte.simbot.ExceptionProcessor
import love.forte.simbot.tencentguild.core.internal.TencentGuildBotImpl
import love.forte.simbot.tencentguild.core.internal.TicketImpl
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@JvmName("newBot")
@JvmOverloads
public fun tencentGuildBot(
    appId: String,
    appKey: String,
    token: String,
    configBlock: TencentGuildBotConfiguration.() -> Unit = {},
): TencentGuildBot {
    return tencentGuildBot(appId, appKey, token, TencentGuildBotConfiguration().also(configBlock))
}

@JvmName("newBot")
public fun tencentGuildBot(
    appId: String,
    appKey: String,
    token: String,
    config: TencentGuildBotConfiguration,
): TencentGuildBot {
    val ticket = TicketImpl(appId, appKey, token)
    return TencentGuildBotImpl(ticket, config)
}


/**
 * 对于一个Bot的配置信息。
 * 如果在配置bot之后对内容进行后续修改，可能会影响到当前bot的使用。
 */
@Suppress("MemberVisibilityCanBePrivate")
public open class TencentGuildBotConfiguration {
    
    /**
     * Context.
     *
     * 如果存在Job，则会被作为parentJob。
     */
    public var coroutineContext: CoroutineContext = EmptyCoroutineContext
    
    /**
     * 分片总数。如果为0，则会通过 [love.forte.simbot.tencentguild.api.GatewayWithShard] 来决定分片结果。
     */
    public var totalShard: Int = 0
    
    
    /**
     * 异常处理器。
     */
    public var exceptionHandler: ExceptionProcessor<Unit>? = null
    
    
    /**
     * 得到所需的所有分片。函数参数为目前全部的shared（如果 [totalShard] > 0, 那么此值为 [totalShard], 否则为通过 [love.forte.simbot.tencentguild.api.GatewayApis.Shared] 得到的建议分片数。）
     *
     * 你可以通过 [shardIterFactory] 来提供当前bot所需要连接的所有分片，且会通过 [intentsForShardFactory] 来决定每一个分片对应的 intent 值。
     *
     * 如果 [shardIterFactory] 为 null，则代表使用 `0 until [totalShard]`，即当前bot连接全部分片。
     */
    public var shardIterFactory: (Int) -> IntIterator = { (0 until it).iterator() }
    
    /**
     * 根据一个分片信息，得到这个分片下需要监听的事件类型。
     *
     * 默认情况下直接为全部的 intents。
     *
     * @see Intents
     */
    public var intentsForShardFactory: (Int) -> Intents = {
        EventSignals.allIntents
    }
    
    /**
     * 根据一个分片信息，得到这个分片下需要监听的事件类型。
     *
     * 相对于直接配置 [intentsForShardFactory], 此方法的配置中，其返回值是 [Int] 而不是 [Intents]。
     *
     * @see intentsForShardFactory
     */
    public inline fun intentsForShardFactoryAsInt(crossinline factory: (Int) -> Int) {
        intentsForShardFactory = { s -> Intents(factory(s)) }
    }
    
    public var clientPropertiesFactory: (Int) -> Signal.Identify.Data.Prop = {
        val os = System.getProperty("os.name", "windows")
        Signal.Identify.Data.Prop(
            os = os, browser = "BROWSER", device = "DEVICE"
        )
    }
    
    /**
     * 提供一个 [HttpClient] 实例用于内部的api请求和ws请求。
     *
     * 因为需要ws请求，因此你需要 `install(WebSockets)`
     *
     */
    public var httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(defaultJson)
        }
        // for install web socket
        install(WebSockets)
    }
    
    /**
     * 请求的服务器地址。默认为 [TencentGuildApi.URL]. 即正式地址。
     */
    public var serverUrl: Url = TencentGuildApi.URL
    
    
    /**
     * 使用的数据解码器。
     *
     * 如果有必要，记得重新设置 [httpClient].
     *
     */
    public var decoder: Json = defaultJson
    
    
    public companion object {
        private val defaultJson = Json {
            isLenient = true
            ignoreUnknownKeys = true
            classDiscriminator = "\$t_"
        }
    }
}

