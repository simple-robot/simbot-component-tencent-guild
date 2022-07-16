package love.forte.simbot.component.tencentguild.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import love.forte.simbot.tencentguild.TencentGuildBotConfiguration

@kotlinx.serialization.Serializable
public open class TencentGuildComponentBotConfiguration : TencentGuildBotConfiguration() {
    
    /**
     * 公域或私域类型。
     *
     * 某些情况下可能会根据此类型对内部策略进行调整。
     *
     */
    public var botDomainType: BotDomainType = BotDomainType.PUBLIC
    
    
  
}


/**
 * bot的作用域类型。
 */
@kotlinx.serialization.Serializable(BotTypeSerializer::class)
public enum class BotDomainType {
    /** 公域类型 */
    PUBLIC,
    /** 私域类型 */
    PRIVATE
}


private object BotTypeSerializer : KSerializer<BotDomainType> {
    override fun deserialize(decoder: Decoder): BotDomainType {
        val name = decoder.decodeString()
        return BotDomainType.valueOf(name.uppercase())
    }
    
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BotTypeSerializer", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: BotDomainType) {
        encoder.encodeString(value.name)
    }
}