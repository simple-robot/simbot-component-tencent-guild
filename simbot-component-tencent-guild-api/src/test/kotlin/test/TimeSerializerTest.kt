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

package test

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import love.forte.simbot.Timestamp
import love.forte.simbot.tencentguild.TimestampISO8601Serializer
import love.forte.simbot.toTimestamp
import java.time.Instant
import kotlin.test.Test


/**
 *
 * @author ForteScarlet
 */
class TimeSerializerTest {

    @Test
    fun test() {
        val j1 = Json.encodeToString(A.serializer(), A(Instant.now().toTimestamp()))
        println(j1)
      
        val j2 = Json.encodeToString(A.serializer(), A(Timestamp.NotSupport))
        println(j2)
    
        println(Json.decodeFromString(A.serializer(), j1))
        println(Json.decodeFromString(A.serializer(), j2))
        println(Json.decodeFromString(A.serializer(), """{"timestamp": "2022-07-13T00:00:00+08:00"}"""))

    }

}

@Serializable
data class A(@Serializable(TimestampISO8601Serializer::class) val timestamp: Timestamp)