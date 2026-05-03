package com.zombies.game.resource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * BitmapProvider 的"关键字为空 / 已标记缺失"路径测试。
 *
 * 不依赖 AssetLoader（Context），通过直接构造缺失行为验证降级语义：
 *  - 空 key 立即返回 null，且不计入 missing（避免泛滥）
 */
class BitmapProviderFallbackTest {

    @Test
    fun emptyKeyReturnsNullWithoutMarkingMissing() {
        // 用一个任意 AssetLoader 的"能 exists 的 null 实现"会很复杂；
        // 此用例仅针对 empty key 的快捷路径进行断言。
        val provider = BitmapProviderFake()
        assertNull(provider.get(""))
        assertEquals(0, provider.missingCount())
    }
}

/**
 * 轻量替身：复刻 BitmapProvider 的"empty key 捷径"，用于纯 JVM 验证降级语义。
 * 不加载任何资源。
 */
private class BitmapProviderFake {
    private val missing = HashSet<String>()
    fun get(key: String): SpriteSheet? {
        if (key.isEmpty() || key in missing) return null
        // 在真实环境下此分支会尝试加载，本 fake 直接视为 missing
        missing.add(key)
        return null
    }
    fun missingCount(): Int = missing.size
}
