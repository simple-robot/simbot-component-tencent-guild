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

package love.forte.simbot.component.tencentguild

import io.ktor.http.*
import kotlinx.serialization.Serializable
import love.forte.simbot.*
import love.forte.simbot.application.ApplicationConfiguration
import love.forte.simbot.application.EventProviderAutoRegistrarFactory
import love.forte.simbot.application.EventProviderFactory
import love.forte.simbot.bot.BotManager
import love.forte.simbot.bot.BotVerifyInfo
import love.forte.simbot.bot.ComponentMismatchException
import love.forte.simbot.component.tencentguild.internal.TencentGuildBotManagerImpl
import love.forte.simbot.component.tencentguild.internal.TencentGuildComponentBotConfiguration
import love.forte.simbot.event.EventProcessor
import love.forte.simbot.tencentguild.EventSignals
import love.forte.simbot.tencentguild.Intents
import love.forte.simbot.tencentguild.TencentGuildApi
import love.forte.simbot.tencentguild.TencentGuildBotConfiguration
import org.slf4j.Logger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


/**
 *
 * QQ频道BOT的bot管理器。
 *
 * [TencentGuildBotManager] 不允许注册相同 `appId` 的bot。
 *
 * @author ForteScarlet
 */
public abstract class TencentGuildBotManager : BotManager<TencentGuildComponentBot>() {
    public abstract val eventProcessor: EventProcessor
    protected abstract val logger: Logger
    abstract override val component: TencentGuildComponent
    public abstract val configuration: TencentGuildBotManagerConfiguration
    
    
    /**
     * 注册一个Bot的信息，并使用默认配置。
     */
    override fun register(verifyInfo: BotVerifyInfo): TencentGuildComponentBot {
        val serializer = TencentBotViaBotFileConfiguration.serializer()
        
        val component = verifyInfo.componentId
        
        val currentComponent = this.component.id
        if (component != currentComponent) {
            logger.debug(
                "[{}] mismatch: [{}] != [{}]", verifyInfo.name, component, currentComponent
            )
            throw ComponentMismatchException("[$component] != [$currentComponent]")
        }
        val configuration = verifyInfo.decode(serializer)
        
        // no config
        return register(configuration.appId, configuration.appKey, configuration.token, configuration::includeConfig)
    }
    
    /**
     * 通过所需信息注册一个bot。
     */
    public abstract fun register(
        appId: String,
        appKey: String,
        token: String,
        block: TencentGuildComponentBotConfiguration.() -> Unit = {},
    ): TencentGuildComponentBot
    
    /**
     * [TencentGuildBotManager] 的构建工厂。
     */
    public companion object Factory :
        EventProviderFactory<TencentGuildBotManager, TencentGuildBotManagerConfiguration> {
        override val key: Attribute<TencentGuildBotManager> = attribute("SIMBOT.TCG")
        
        override suspend fun create(
            eventProcessor: EventProcessor,
            components: List<Component>,
            applicationConfiguration: ApplicationConfiguration,
            configurator: TencentGuildBotManagerConfiguration.() -> Unit,
        ): TencentGuildBotManager {
            val configuration = TencentGuildBotManagerConfigurationImpl().also {
                it.parentCoroutineContext = applicationConfiguration.coroutineContext
                configurator(it)
            }
            
            // find component
            val component = components.find { it.id == TencentGuildComponent.ID_VALUE } as? TencentGuildComponent
                ?: throw NoSuchComponentException("component id [${TencentGuildComponent.ID_VALUE}], and type of TencentGuildComponent.")
            
            return TencentGuildBotManagerImpl(eventProcessor, configuration, component).also {
                configuration.useBotManager(it)
            }
        }
        
        @JvmStatic
        @Deprecated("Use bot manager in Application.")
        public fun newInstance(
            processor: EventProcessor,
            configuration: TencentGuildBotManagerConfiguration,
        ): TencentGuildBotManager {
            return TencentGuildBotManagerImpl(processor, configuration, TencentGuildComponent())
        }
    }
}

@DslMarker
@Retention(AnnotationRetention.BINARY)
internal annotation class TencentGuildBotManagerConfigurationDsl


/**
 * 得到一个BotManager.
 */
@Suppress("DEPRECATION")
@Deprecated("Use bot manager in Application.")
public fun tencentGuildBotManager(
    processor: EventProcessor,
    block: TencentGuildBotManagerConfiguration.() -> Unit = { },
): TencentGuildBotManager {
    return TencentGuildBotManager.newInstance(processor, TencentGuildBotManagerConfigurationImpl().also(block))
}


/**
 * [TencentGuildBotManager] 使用的配置类描述。
 */
public interface TencentGuildBotManagerConfiguration {
    
    /**
     * 对所有bot的配置信息进行统一处理的函数。
     * 如果直接对 [botConfigure] 进行赋值，则会覆盖之前的配置。
     *
     * 如果想要以_追加_的形式进行配置，考虑使用同名的函数 [botConfigure].
     *
     * e.g.
     * ```kotlin
     * botConfigure = { appId, appKey, token -> /* ... */ }
     * ```
     */
    public var botConfigure: TencentGuildBotConfiguration.(appId: String, appKey: String, token: String) -> Unit
    
    /**
     * 对所有bot的配置信息进行统一处理的函数。
     * 使用当前函数会保留之前的配置。
     *
     * 如果想直接覆盖配置并抛弃之前的配置，考虑使用同名的属性 [botConfigure].
     *
     * e.g.
     * ```kotlin
     * botConfigure { appId, appKey, token -> /* ... */ }
     * ```
     */
    @TencentGuildBotManagerConfigurationDsl
    public fun botConfigure(configure: TencentGuildBotConfiguration.(appId: String, appKey: String, token: String) -> Unit)
    
    /**
     * 当前botManager使用的协程上下文。初始值为 [ApplicationConfiguration] 所提供的上下文。
     *
     */
    public var parentCoroutineContext: CoroutineContext
    
    
    /**
     * 提供 [appId]、[appKey]、[token] 和 [配置信息][botConfiguration] 来预注册一个bot。
     *
     * @param onBot 当bot被注册后的回调函数。
     */
    @TencentGuildBotManagerConfigurationDsl
    public fun register(
        appId: String, appKey: String, token: String,
        botConfiguration: TencentGuildBotConfiguration.() -> Unit = {},
        onBot: suspend (TencentGuildComponentBot) -> Unit,
    )
    
    
}


/**
 * [TencentGuildBotManager] 的自动注册工厂。
 */
public class TencentGuildBotManagerAutoRegistrarFactory :
    EventProviderAutoRegistrarFactory<TencentGuildBotManager, TencentGuildBotManagerConfiguration> {
    override val registrar: TencentGuildBotManager.Factory get() = TencentGuildBotManager
}


/**
 * [TencentGuildBotManager] 使用的配置类。
 */
@Suppress("MemberVisibilityCanBePrivate")
private class TencentGuildBotManagerConfigurationImpl : TencentGuildBotManagerConfiguration {
    override var parentCoroutineContext: CoroutineContext = EmptyCoroutineContext
    
    override var botConfigure: TencentGuildBotConfiguration.(appId: String, appKey: String, token: String) -> Unit =
        { _, _, _ -> }
    
    override fun botConfigure(configure: TencentGuildBotConfiguration.(appId: String, appKey: String, token: String) -> Unit) {
        botConfigure.also { old ->
            botConfigure = { appId, appKey, token ->
                old(appId, appKey, token)
                configure(appId, appKey, token)
            }
        }
    }
    
    
    private var botManagerConfig: suspend (TencentGuildBotManager) -> Unit = {}
    
    private fun addBotManagerConfig(block: suspend (TencentGuildBotManager) -> Unit) {
        botManagerConfig.also { old ->
            botManagerConfig = {
                old(it)
                block(it)
            }
        }
    }
    
    override fun register(
        appId: String,
        appKey: String,
        token: String,
        botConfiguration: TencentGuildBotConfiguration.() -> Unit,
        onBot: suspend (TencentGuildComponentBot) -> Unit,
    ) {
        addBotManagerConfig { manager ->
            manager.register(appId, appKey, token, botConfiguration)
        }
    }
    
    suspend fun useBotManager(botManager: TencentGuildBotManagerImpl) {
        botManagerConfig(botManager)
    }
    
}

/**
 * bot配置文件所对应的配置类，
 *
 * 通过由配置文件读取而来的信息来对指定Bot进行信息配置。
 */
@OptIn(InternalSimbotApi::class)
@Suppress("MemberVisibilityCanBePrivate")
@Serializable
public data class TencentBotViaBotFileConfiguration(
    /**
     * app id.
     */
    val appId: String,
    
    /**
     * app key.
     */
    val appKey: String,
    
    /**
     * token.
     */
    val token: String,
    
    
    @Deprecated("Use config.totalShard") val totalShard: Int? = null,
    @Deprecated("Use config.intentValues") val intentValues: Map<Int, Int> = emptyMap(),
    @Deprecated("Use config.defaultIntents") val defaultIntents: List<String> = emptyList(),
    @Deprecated("Use config.serverUrl") val serverUrl: String? = null,
    
    
    val config: Config = DEFAULT_CONFIG,
    
    ) {
    
    @Serializable
    @InternalSimbotApi
    public data class Config(
        /**
         * 分片总数。
         * @see [TencentGuildBotConfiguration.totalShard]
         */
        val totalShard: Int? = null,
        
        /**
         * 分片策略。key为分片值，
         * value为对应分片下所需的 intent.
         *
         */
        val intentValues: Map<Int, Int> = emptyMap(),
        
        /**
         * 默认的 [Intents]. 如果对应分片下 [intentValues] 无法找到指定的 intent, 则使用此默认值。
         */
        val defaultIntents: List<String> = EventSignals.intents.keys.toList(),
        
        /**
         * 服务器路径地址。
         * @see TencentGuildApi.URL_STRING
         */
        val serverUrl: String? = null,
    )
    
    internal val defaultIntentsValue: Intents
        get() {
            return config.defaultIntents.distinct().map { v ->
                EventSignals.intents[v] ?: kotlin.runCatching {
                    Intents(v.toInt())
                }.getOrElse {
                    throw SimbotIllegalArgumentException(
                        """Cannot resolve '$v' to intent value.
                    |You can use a intent value, or use name value in: ${EventSignals.intents.keys}
                """.trimMargin(), it
                    )
                }
            }.reduce { acc, intents -> acc + intents }
        }
    
    
    @Suppress("DEPRECATION")
    internal fun includeConfig(configuration: TencentGuildComponentBotConfiguration) {
        if (totalShard != null) {
            logger.warn("Config [totalShard] is deprecated. please use [config.totalShard]")
        }
        if (config.totalShard != null) {
            configuration.totalShard = config.totalShard
        }
        
       
        configuration.intentsForShardFactory =
            { shard -> config.intentValues[shard]?.let { Intents(it) } ?: defaultIntentsValue }
        if (intentValues.isNotEmpty()) {
            logger.warn("Config [intentValues] is deprecated. please use [config.intentValues]")
        }
        
        
        if (serverUrl != null) {
            logger.warn("Config [serverUrl] is deprecated. please use [config.serverUrl]")
        }
        if (config.serverUrl != null) {
            configuration.serverUrl = Url(config.serverUrl)
        }
    }
    
    public companion object {
        private val logger = LoggerFactory.getLogger<TencentBotViaBotFileConfiguration>()
        private val DEFAULT_CONFIG = Config()
    }
}

