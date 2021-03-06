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

package love.forte.simbot.tencentguild.api.message

import io.ktor.http.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import love.forte.simbot.CharSequenceID
import love.forte.simbot.ID
import love.forte.simbot.Timestamp
import love.forte.simbot.literal
import love.forte.simbot.tencentguild.*
import love.forte.simbot.tencentguild.api.RouteInfoBuilder
import love.forte.simbot.tencentguild.api.TencentApi
import love.forte.simbot.tencentguild.internal.TencentUserInfoImpl


/**
 *
 * [发送消息](https://bot.q.qq.com/wiki/develop/api/openapi/message/post_messages.html)
 *
 * - 要求操作人在该子频道具有发送消息的权限。
 * - 发送成功之后，会触发一个创建消息的事件。
 * - 被动回复消息有效期为 5 分钟
 * - 主动推送消息每日每个子频道限 2 条
 * - 发送消息接口要求机器人接口需要链接到websocket gateway 上保持在线状态
 *
 * <hr>
 *
 * [发送模板消息](https://bot.q.qq.com/wiki/develop/api/openapi/message/post_ark_messages.html)
 *
 * - 要求操作人在该子频道具有发送消息和 模板消息 的权限。
 * - 调用前需要先申请消息模板，这一步会得到一个模板id，在请求时填在ark.template_id上
 * - 发送成功之后，会触发一个创建消息的事件。
 * - 可用模板参考 [可用模板](https://bot.q.qq.com/wiki/develop/api/openapi/message/message_template.html)
 *
 *
 * @author ForteScarlet
 */
public class MessageSendApi(channelId: ID, override val body: TencentMessageForSending) :
    TencentApi<TencentMessage>() {
    @JvmOverloads
    public constructor(channelId: ID, content: String, msgId: ID? = null) : this(
        channelId,
        TencentMessageForSending(content = content, msgId = msgId)
    )
    
    @JvmOverloads
    public constructor(channelId: ID, embed: TencentMessage.Embed, msgId: ID? = null) : this(
        channelId,
        TencentMessageForSending(embed = embed, msgId = msgId)
    )
    
    @JvmOverloads
    public constructor(channelId: ID, ark: TencentMessage.Ark, msgId: ID? = null) : this(
        channelId,
        TencentMessageForSending(ark = ark, msgId = msgId)
    )
    
    // POST /channels/{channel_id}/messages
    private val path: List<String> = listOf("channels", channelId.literal, "messages")
    
    override val resultDeserializer: DeserializationStrategy<out TencentMessage>
        get() = SendMessageResult.serializer()
    
    override val method: HttpMethod
        get() = HttpMethod.Post
    
    override fun route(builder: RouteInfoBuilder) {
        builder.apiPath = path
    }
    
    
}

@Serializable
internal data class SendMessageResult(
    override val id: CharSequenceID,
    @SerialName("channel_id")
    override val channelId: CharSequenceID,
    @SerialName("guild_id")
    override val guildId: CharSequenceID,
    override val content: String,
    @Serializable(TimestampISO8601Serializer::class)
    override val timestamp: Timestamp,
    @SerialName("edited_timestamp")
    override val editedTimestamp: Timestamp = Timestamp.NotSupport,
    @SerialName("mention_everyone")
    override val mentionEveryone: Boolean = false,
    override val author: TencentUserInfoImpl,
    override val attachments: List<TencentMessage.Attachment> = emptyList(),
    override val embeds: List<TencentMessage.Embed> = emptyList(),
    override val mentions: List<TencentUserInfoImpl> = emptyList(),
    override val ark: TencentMessage.Ark? = null,
) : TencentMessage {
    @Transient
    override val member: AuthorAsMember = AuthorAsMember(guildId, author)
}

internal data class AuthorAsMember(
    override val guildId: ID?,
    private val author: TencentUserInfoImpl,
) : TencentMemberInfo {
    override val user: TencentUserInfo get() = author
    override val nick: String get() = ""
    override val roleIds: List<ID> = listOf(TencentRoleInfo.DefaultRole.ALL_MEMBER.code.ID)
    override val joinedAt: Timestamp
        get() = Timestamp.NotSupport
}