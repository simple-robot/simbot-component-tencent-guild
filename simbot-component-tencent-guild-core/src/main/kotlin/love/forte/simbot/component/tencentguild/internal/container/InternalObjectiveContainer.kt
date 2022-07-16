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

package love.forte.simbot.component.tencentguild.internal.container

import love.forte.simbot.component.tencentguild.internal.TencentMemberImpl
import java.util.concurrent.ConcurrentMap

/**
 *
 * 内部使用的、用于记录/缓存某对象的容器。
 *
 * 用于内部对象对部分信息的更新，例如 [TencentMemberImpl]。
 *
 * 此类型面向内部，且各实现类可能会存在大量的特殊实现。
 *
 * 对获取的定义，可以**类似的**看作一个 [Map]。但是此类型不实现 [Map]。
 *
 * [InternalObjectiveContainer] 的实现需要保证操作的原子性。
 *
 * @param V 容器中所存储的主要类型。
 *
 * @author ForteScarlet
 */
internal interface InternalObjectiveContainer<K, V> {
    
    /**
     * 尝试获取容器中的某个指定元素。
     */
    operator fun get(key: K): V?
    
    /**
     * 得到所有的key。
     */
    val keys: Set<K>
    
    /**
     * 得到所有的元素。
     */
    val values: Collection<V>
    
    /**
     * 直接填充或覆盖容器中某个指定元素。
     *
     * @return 被替换的元素
     */
    operator fun set(key: K, value: V): V?
    
    /**
     * 直接移除某个指定元素。
     */
    fun remove(key: K): V?
}


internal abstract class InternalConcurrentMapObjectiveContainer<K, V> : InternalObjectiveContainer<K, V> {
    protected abstract val container: ConcurrentMap<K, V>
    
    override fun get(key: K): V? = container[key]
    
    override val keys: Set<K> get() = container.keys
    override val values: Collection<V> get() = container.values
    
    override fun set(key: K, value: V): V? = container.put(key, value)
    
    override fun remove(key: K): V? = container.remove(key)
    
}


