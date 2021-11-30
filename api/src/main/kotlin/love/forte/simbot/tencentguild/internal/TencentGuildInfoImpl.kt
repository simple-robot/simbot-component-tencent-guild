package love.forte.simbot.tencentguild.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.CharSequenceID
import love.forte.simbot.Timestamp
import love.forte.simbot.tencentguild.TencentGuildInfo
import love.forte.simbot.tencentguild.TimestampISO8601Serializer


/**
 *
 *  [频道对象](https://bot.q.qq.com/wiki/develop/api/openapi/guild/model.html)
 *
 * @author ForteScarlet
 */
@Serializable
internal data class TencentGuildInfoImpl(
    override val id: CharSequenceID,
    override val name: String,
    override val icon: String,
    @SerialName("owner_id")
    override val ownerId: CharSequenceID,
    @SerialName("owner")
    override val isBotOwner: Boolean,
    @SerialName("member_count")
    override val memberCount: Int,
    @SerialName("max_members")
    override val maxMembers: Int,
    override val description: String,
    @SerialName("joined_at")
    @Serializable(TimestampISO8601Serializer::class)
    override val joinedAt: Timestamp,
    @SerialName("union_world_id")
    override val unionWorldId: String? = null,
    @SerialName("union_org_id")
    override val unionOrgId: String? = null,
) : TencentGuildInfo