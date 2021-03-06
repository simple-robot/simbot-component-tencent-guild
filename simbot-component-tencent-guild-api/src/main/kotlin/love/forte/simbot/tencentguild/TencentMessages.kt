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

import kotlinx.serialization.*
import love.forte.simbot.*
import love.forte.simbot.tencentguild.internal.*


/**
 * [消息对象(Message)](https://bot.q.qq.com/wiki/develop/api/openapi/message/model.html)
 *
 * 消息对象的完整定义, 是用于接收的消息。
 *
 */
public interface TencentMessage {
    /**
     * 消息 id
     */
    public val id: ID

    /**
     * 子频道 id
     */
    public val channelId: ID

    /**
     * 频道 id
     */
    public val guildId: ID

    /**
     * 消息内容
     */
    public val content: String

    /**
     * ISO8601 timestamp	消息创建时间
     */
    public val timestamp: Timestamp

    /**
     * ISO8601 timestamp	消息编辑时间
     */
    public val editedTimestamp: Timestamp

    /**
     * 是否是@全员消息
     */
    public val mentionEveryone: Boolean

    /**
     * 消息创建者
     */
    public val author: TencentUserInfo

    /**
     * MessageAttachment 对象数组	附件
     */
    public val attachments: List<Attachment>

    /**
     * embeds	MessageEmbed 对象数组	embed
     */
    public val embeds: List<Embed>

    /**
     * User 对象数组	消息中@的人
     */
    public val mentions: List<TencentUserInfo>

    /**
     * Member 对象	消息创建者的member信息
     */
    public val member: TencentMemberInfo

    /**
     * ark消息对象	ark消息
     */
    public val ark: Ark?


    /**
     * [https://bot.q.qq.com/wiki/develop/api/openapi/message/model.html#messageembed]
     */
    @Serializable
    public data class Embed(
        /**
         * 标题
         */
        public val title: String,

        /**
         * 描述
         */
        public val description: String,

        /**
         * 消息弹窗内容
         */
        public val prompt: String,

        /**
         * iISO8601 消息创建时间
         */
        public val timestamp: Timestamp,

        /**
         * MessageEmbedField 对象数组	字段信息
         */
        public val fields: List<Field>
    ) {

        /**
         * [https://bot.q.qq.com/wiki/develop/api/openapi/message/model.html#messageembedfield]
         */
        @Serializable
        public data class Field(
            /**
             * 字段名
             */
            public val name: String,

            /**
             * 字段值
             */
            public val value: String,
        )
    }


    /**
     * [https://bot.q.qq.com/wiki/develop/api/openapi/message/model.html#messageattachment]
     */
    @Serializable
    public data class Attachment(
        /**
         * 下载地址
         */
        public val url: String
    )


    /**
     * [https://bot.q.qq.com/wiki/develop/api/openapi/message/model.html#messageark]
     */
    @Serializable
    public data class Ark(
        /**
         * ark模板id（需要先申请）
         */
        @SerialName("template_id")
        @Serializable(ID.AsCharSequenceIDSerializer::class)
        public val templateId: ID,

        /**
         * kv值列表
         */
        public val kv: List<Kv> = emptyList()
    ) {


        /**
         * [https://bot.q.qq.com/wiki/develop/api/openapi/message/model.html#messagearkkv]
         */
        @Serializable
        public data class Kv(
            public val key: String,
            public val value: String? = null,
            public val obj: List<Obj> = emptyList()
        )


        /**
         * [https://bot.q.qq.com/wiki/develop/api/openapi/message/model.html#messagearkobj]
         */
        @Serializable
        public data class Obj(
            /** ark obj kv列表 */
            @SerialName("obj_kv")
            public val objKv: List<Kv> = emptyList()
        ) {

            /**
             * https://bot.q.qq.com/wiki/develop/api/openapi/message/model.html#messagearkobjkv
             */
            @Serializable
            public data class Kv(public val key: String, public val value: String)
        }

    }


    public companion object {
        internal val serializer: KSerializer<out TencentMessage> = TencentMessageImpl.serializer()
        //internal val sendSerializer: KSerializer<out TencentMessage> = TencentMessageImplSerializer(TencentMessageImpl.serializer())
    }
}



