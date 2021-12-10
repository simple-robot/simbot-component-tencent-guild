import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import love.forte.simbot.ID
import love.forte.simbot.component.tencentguild.TencentGuildBot
import love.forte.simbot.component.tencentguild.tencentGuildBotManager
import love.forte.simbot.core.event.coreListenerManager
import love.forte.simbot.core.event.listen
import love.forte.simbot.event.*
import love.forte.simbot.tencentguild.EventSignals


private val listenerManager = coreListenerManager {
    interceptors {
        processingIntercept(114514.ID) {
            println("Processing Intercept 1 start")
            it.proceed().also {
                println("Processing Intercept 1 end")
            }
        }
        listenerIntercept(1.ID) {
            println("Listener Intercept 2 start")
            it.proceed().also {
                println("Listener Intercept 2 end")
            }
        }
        listenerIntercept(2.ID) {
            println("Listener Intercept 3 start")
            it.proceed().also {
                println("Listener Intercept 3 end")
            }
        }
        listenerIntercept(3.ID) {
            println("Listener Intercept 4 start")
            it.proceed().also {
                println("Listener Intercept 4 end")
            }
        }
    }
}


private val botManager = tencentGuildBotManager(listenerManager) {
    botConfigure = { _, _, _ ->
        intentsForShardFactory = { EventSignals.AtMessages.intents }
    }
}


suspend fun main() {
    listenerManager.listen(eventKey = ChannelMessageEvent) { context, event ->
        // do

        null // result
    }
    val bot: TencentGuildBot = botManager.register("", "", "") {
        intentsForShardFactory = { EventSignals.AtMessages.intents }
    }

    val id = bot.id

    println(botManager.get(id))

    bot.launch {
        delay(10_000)
        bot.cancel()
    }

    bot.start()

    bot.join()

    println(botManager.get(id))


}


fun <E : Event> listener(id: ID, type: Event.Key<E>, invoker: suspend (EventProcessingContext, E) -> Any?): EventListener =
    object : EventListener {
        override val id: ID get() = id
        override fun isTarget(eventType: Event.Key<*>): Boolean = eventType.isSubFrom(type)
        override suspend fun invoke(context: EventProcessingContext): EventResult {
            val result = invoker(context, type.safeCast(context.event)!!)
            return EventResult.of(result)
        }
    }
