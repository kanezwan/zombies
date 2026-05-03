package com.zombies.game.core

/**
 * 简单对象池，用于子弹 / 阳光 / 伤害数字等高频创建销毁对象。
 *
 * 注意：归还对象前应由调用方 reset 其状态；池不做内部清理。
 */
class ObjectPool<T : Any>(
    private val factory: () -> T,
    private val maxSize: Int = 64
) {
    private val pool = ArrayDeque<T>(maxSize.coerceAtMost(16))

    fun acquire(): T = if (pool.isEmpty()) factory() else pool.removeLast()

    fun release(obj: T) {
        if (pool.size < maxSize) pool.addLast(obj)
    }

    fun size(): Int = pool.size

    fun clear() = pool.clear()
}
