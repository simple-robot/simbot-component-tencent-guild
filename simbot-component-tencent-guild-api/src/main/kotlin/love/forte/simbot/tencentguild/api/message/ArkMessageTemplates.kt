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

import love.forte.simbot.*
import love.forte.simbot.tencentguild.*


/**
 * 官方提供的ark消息模板。
 *
 * see https://bot.q.qq.com/wiki/develop/api/openapi/message/message_template.html
 *
 */
public sealed class ArkMessageTemplates {

    public abstract val ark: TencentMessage.Ark

    /**
     * ## 23 链接+文本列表模板
     *
     * see [23 链接+文本列表模板](https://bot.q.qq.com/wiki/develop/api/openapi/message/template/template_23.html)
     *
     * @param desc
     * @param prompt
     * @param list
     */
    public class TextLinkList(
        desc: String,
        prompt: String,
        list: List<Desc> = emptyList()
    ) : ArkMessageTemplates() {
        private companion object {
            val ID = 23.ID
        }

        override val ark: TencentMessage.Ark = buildArk(ID) {
            kvs {
                kv("#DESC#", desc)
                kv("#PROMPT#", prompt)
                if (list.isNotEmpty()) {
                    kv("#LIST#") {
                        list.forEach { desc ->
                            obj {
                                kv("desc", desc.desc)
                                desc.link?.also { link ->
                                    kv("link", link)
                                }
                            }
                        }
                    }
                }

            }
        }

        public data class Desc(public val desc: String, public val link: String? = null)
    }


    /**
     * ## 24 文本+缩略图模板
     *
     * see [24 文本+缩略图模板](https://bot.q.qq.com/wiki/develop/api/openapi/message/template/template_24.html)
     *
     * @param desc 描述
     * @param prompt 提示文本
     * @param title 标题
     * @param metaDesc 详情描述
     * @param img 图片链接
     * @param link 跳转链接
     * @param subtitle 来源
     */
    public class TextThumbnail(
        desc: String,
        prompt: String,
        title: String,
        metaDesc: String,
        img: String,
        link: String,
        subtitle: String,
    ) : ArkMessageTemplates() {
        private companion object {
            val ID = 24.ID
        }

        override val ark: TencentMessage.Ark = buildArk(ID) {
            kv("#DESC#", desc)
            kv("#PROMPT#", prompt)
            kv("#TITLE#", title)
            kv("#METADESC#", metaDesc)
            kv("#IMG#", img)
            kv("#LINK#", link)
            kv("#SUBTITLE#", subtitle)
        }
    }


    /**
     * ## 37 大图模板
     *
     * see [37 大图模板](https://bot.q.qq.com/wiki/develop/api/openapi/message/template/template_37.html)
     *
     * @param prompt 提示消息
     * @param metaTitle 标题
     * @param metaSubtitle 子标题
     * @param metaCover 大图，尺寸为 975*540
     * @param metaUrl 跳转链接
     */
    public class BigImg(
        prompt: String,
        metaTitle: String,
        metaSubtitle: String,
        metaCover: String,
        metaUrl: String,
    ) : ArkMessageTemplates() {
        private companion object {
            val ID = 37.ID
        }

        override val ark: TencentMessage.Ark = buildArk(ID) {
            kv("#PROMPT#", prompt)
            kv("#METATITLE#", metaTitle)
            kv("#METASUBTITLE#", metaSubtitle)
            kv("#METACOVER#", metaCover)
            kv("#METAURL#", metaUrl)
        }
    }


}




