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

import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable


@Suppress("MemberVisibilityCanBePrivate")
public class TencentApiException : IllegalStateException {
    /**
     * 异常响应的content信息。
     */
    public val info: ErrInfo?
    
    /**
     * http/ws协议本身的错误响应码。
     * 假如不存在（例如ws断开时没有提供 [CloseReason]）则为 -1。
     */
    public val value: Int
    
    /**
     * http/ws协议本身的错误响应描述。
     */
    public val description: String
    
    public constructor(value: Int, description: String) : super("$value: $description") {
        this.info = null
        this.value = value
        this.description = description
    }
    
    public constructor(
        info: ErrInfo,
        value: Int,
        description: String,
    ) : super("$value: $description; response info: $info") {
        this.info = info
        this.value = value
        this.description = description
    }
    
}

public inline fun ErrInfo.err(codeBlock: () -> HttpStatusCode): Nothing {
    val code = codeBlock()
    throw TencentApiException(this, code.value, code.description)
}

/**
 * 如果返回值不是成功的响应码，[ErrInfo] 则为统一的错误码响应格式。
 *
 */
@Suppress("MemberVisibilityCanBePrivate")
@Serializable
public data class ErrInfo(val code: Int, val message: String)


@Suppress("NOTHING_TO_INLINE")
public inline fun CloseReason?.err(e: Throwable? = null): Nothing {
    if (this == null) {
        if (e != null) {
            throw TencentApiException(-1, "No reason").initCause(e)
        } else {
            throw TencentApiException(-1, "No reason")
        }
    }
    val known = knownReason
    val message = message
    if (known != null) {
        if (e != null) {
            throw TencentApiException(
                known.code.toInt(),
                "${known.name}: $message"
            ).initCause(e)
        } else {
            throw TencentApiException(known.code.toInt(), "${known.name}: $message")
        }
    } else {
        if (e != null) {
            throw TencentApiException(code.toInt(), message).initCause(e)
        } else {
            throw TencentApiException(code.toInt(), message)
        }
    }
}