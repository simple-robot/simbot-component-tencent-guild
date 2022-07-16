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

import love.forte.simbot.component.tencentguild.internal.container.MemoryInternalTcgChannelCategoryContainer
import love.forte.simbot.component.tencentguild.internal.container.MemoryInternalTcgChannelContainer
import love.forte.simbot.component.tencentguild.internal.container.memory
import love.forte.simbot.component.tencentguild.internal.event.eventSignalParsers
import love.forte.simbot.component.tencentguild.internal.event.findOrCreateGuildImpl
import love.forte.simbot.event.pushIfProcessable
import love.forte.simbot.literal
import love.forte.simbot.tencentguild.EventSignals
import love.forte.simbot.tencentguild.TencentChannelInfo
import love.forte.simbot.tencentguild.TencentGuildInfo
import love.forte.simbot.tencentguild.isGrouping


internal fun TencentGuildComponentBotImpl.registerEventProcessor() {
    registerEventPreProcessor()
    registerNormalEventProcessor()
}

/**
 * 注册预处理事件，用于监听各类'变化'事件并同步数据。
 */
private fun TencentGuildComponentBotImpl.registerEventPreProcessor() {
    source.preProcessor { _, decoded ->
        when (type) {
            EventSignals.Guilds.GuildCreate.type -> onGuildCreate(decoded)
            EventSignals.Guilds.GuildUpdate.type -> onGuildUpdate(decoded)
            EventSignals.Guilds.GuildDelete.type -> onGuildDelete(decoded)
            EventSignals.Guilds.ChannelCreate.type -> onChannelCreate(decoded)
            EventSignals.Guilds.ChannelUpdate.type -> onChannelUpdate(decoded)
            EventSignals.Guilds.ChannelDelete.type -> onChannelDelete(decoded)
            EventSignals.GuildMembers.GuildMemberAdd.type -> onMemberAdd(decoded)
            EventSignals.GuildMembers.GuildMemberUpdate.type -> onMemberUpdate(decoded)
            EventSignals.GuildMembers.GuildMemberRemove.type -> onMemberRemove(decoded)
        }
    }
}

// region guilds
private suspend fun TencentGuildComponentBotImpl.onGuildCreate(decoded: () -> Any) {
    val eventData = decoded()
    if (eventData is TencentGuildInfo) {
        guildContainer.memory?.computeAndGet(eventData)?.also {
            logger.debug("OnGuildCreate sync: {}", it)
        }
    }
}

private fun TencentGuildComponentBotImpl.onGuildUpdate(decoded: () -> Any) {
    val eventData = decoded()
    if (eventData is TencentGuildInfo) {
        guildContainer.memory?.justGet(eventData.id.literal)?.also { guild ->
            guild.source = eventData
            logger.debug("OnGuildUpdate sync: {}", eventData)
        }
    }
}

private fun TencentGuildComponentBotImpl.onGuildDelete(decoded: () -> Any) {
    val eventData = decoded()
    if (eventData is TencentGuildInfo) {
        guildContainer.memory?.remove(eventData.id.literal)?.also {
            logger.debug("OnGuildDelete sync: {}", eventData)
        }
    }
}
// endregion

// region channels
@Suppress("RedundantSuspendModifier")
private suspend fun TencentGuildComponentBotImpl.onChannelCreate(decoded: () -> Any) {
    val eventData = decoded()
    if (eventData is TencentChannelInfo) {
        val guild = findOrCreateGuildImpl(eventData.guildId) {
            logger.debug(
                "No existing guild with id [{}] found in [onChannelCreate]. Build and save guild {}",
                it.id,
                it
            )
            guildContainer.memory?.container?.set(it.id.literal, it)
        }
        
        guild.channelContainer.memory?.computeAndGet(this, eventData)
    }
}

private suspend fun TencentGuildComponentBotImpl.onChannelUpdate(decoded: () -> Any) {
    val eventData = decoded()
    if (eventData is TencentChannelInfo) {
        val guild = guildContainer.get(eventData.guildId.literal) ?: return
        if (eventData.channelType.isGrouping) {
            guild.channelCategoryContainer.get(eventData.id.literal)?.also { category ->
                category.source = eventData
                logger.debug("OnChannelUpdate sync: {}", eventData)
            } ?: run {
                logger.debug(
                    "OnChannelUpdate, but not sync. guild or channel by event data: [{}] not found.",
                    eventData
                )
            }
        } else {
            guild.channelContainer.get(eventData.id.literal)?.also { channel ->
                channel.source = eventData
                logger.debug("OnChannelUpdate sync: {}", eventData)
            } ?: kotlin.run {
                logger.debug(
                    "OnChannelUpdate, but not sync. guild or channel by event data: [{}] not found.",
                    eventData
                )
            }
        }
    }
}

private suspend fun TencentGuildComponentBotImpl.onChannelDelete(decoded: () -> Any) {
    val eventData = decoded()
    if (eventData is TencentChannelInfo) {
        val guild = guildContainer.get(eventData.guildId.literal) ?: return
        if (eventData.channelType.isGrouping) {
            val categoryContainer =
                guild.channelCategoryContainer as? MemoryInternalTcgChannelCategoryContainer ?: return
            val categoryId = eventData.id.literal
            val removedCategory = categoryContainer.remove(eventData.id.literal)
            logger.debug(
                "OnChannelDelete sync: removed channel category [{}] from event data {}",
                removedCategory,
                eventData
            )
            val channelContainer = guild.channelContainer as? MemoryInternalTcgChannelContainer ?: return
            val values = channelContainer.container.values.iterator()
            while (values.hasNext()) {
                val next = values.next()
                if (next.source.parentId == categoryId) {
                    values.remove()
                    logger.debug(
                        "OnChannelDelete sync: removed channel [{}] (by category(id={})) from event data {}",
                        removedCategory,
                        categoryId,
                        eventData
                    )
                }
            }
        } else {
            val channelContainer = guild.channelContainer as? MemoryInternalTcgChannelContainer ?: return
            val removed = channelContainer.remove(eventData.id.literal)
            logger.debug("OnChannelDelete sync: removed channel [{}] by event data {}", removed, eventData)
        }
    }
}
// endregion

// region members
@Suppress("unused", "UNUSED_PARAMETER")
private fun TencentGuildComponentBotImpl.onMemberAdd(decoded: () -> Any) {
    // nothing.
}

@Suppress("unused", "UNUSED_PARAMETER")
private fun TencentGuildComponentBotImpl.onMemberUpdate(decoded: () -> Any) {
    // nothing.
}

@Suppress("unused", "UNUSED_PARAMETER")
private fun TencentGuildComponentBotImpl.onMemberRemove(decoded: () -> Any) {
    // nothing.
}
// endregion


/**
 * 注册普通的事件处理器。
 */
private fun TencentGuildComponentBotImpl.registerNormalEventProcessor() {
    // process event.
    source.processor { json, decoded ->
        // event processor
        logger.trace("EventSignals.events[{}]: {}", type, EventSignals.events[type])
        EventSignals.events[this.type]?.let { signals ->
            logger.trace("eventSignalParsers[{}]: {}", signals, eventSignalParsers[signals])
            
            eventSignalParsers[signals]?.let { parser ->
                
                logger.trace(
                    "eventProcessor.isProcessable({}): {}", parser.key, eventProcessor.isProcessable(parser.key)
                )
                eventProcessor.pushIfProcessable(parser.key) {
                    parser(
                        bot = this@registerNormalEventProcessor, decoder = json, decoded = decoded, dispatch = this
                    )
                }
            }
        }
    }
}
