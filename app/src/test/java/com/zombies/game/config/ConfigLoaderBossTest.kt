package com.zombies.game.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M11: 验证 [ConfigLoader.parseZombies] 正确解析新增的 `isBoss` 字段。
 */
class ConfigLoaderBossTest {

    @Test
    fun parsesIsBossFlag_whenPresent() {
        val json = """
            {
              "bossdrzomboss": {
                "hp": 8000,
                "speed": 12,
                "attackDamage": 60,
                "attackIntervalMs": 1200,
                "isBoss": true
              }
            }
        """.trimIndent()
        val map = ConfigLoader.parseZombies(json)
        val z = map.getValue("bossdrzomboss")
        assertEquals(8000, z.hp)
        assertEquals(12f, z.speed, 0.001f)
        assertTrue(z.isBoss)
    }

    @Test
    fun defaultsIsBossToFalse_whenMissing() {
        val json = """
            {
              "basic": { "hp": 200, "speed": 22, "attackDamage": 20, "attackIntervalMs": 1000 }
            }
        """.trimIndent()
        val map = ConfigLoader.parseZombies(json)
        assertFalse(map.getValue("basic").isBoss)
    }

    @Test
    fun builtInBossDrzombossHasCorrectStats() {
        // 防御性：验证默认配置里 BOSS_DRZOMBOSS 的关键字段无误（防止后续被误改）
        val boss = ZombieConfig.BOSS_DRZOMBOSS
        assertEquals("bossdrzomboss", boss.type)
        assertEquals(8000, boss.hp)
        assertEquals(12f, boss.speed, 0.001f)
        assertTrue(boss.isBoss)
    }

    @Test
    fun defaultsListIncludesBoss() {
        val byType = ZombieConfig.DEFAULTS.associateBy { it.type }
        assertTrue("DEFAULTS should include bossdrzomboss", byType.containsKey("bossdrzomboss"))
    }
}
