package com.zombies.game.save

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveManagerTest {

    private lateinit var prefs: InMemorySharedPreferences
    private lateinit var sm: SaveManager

    @Before
    fun setUp() {
        prefs = InMemorySharedPreferences()
        sm = SaveManager.from(prefs)
    }

    // ---- 默认状态 ----

    @Test
    fun `default level_1 is unlocked and not cleared`() {
        val p1 = sm.getProgress("level_1")
        assertTrue(p1.unlocked)
        assertFalse(p1.cleared)
        assertNull(p1.bestTimeMs)
        assertNull(p1.bestSunLeft)
    }

    @Test
    fun `default level_2 and level_3 are locked`() {
        assertFalse(sm.getProgress("level_2").unlocked)
        assertFalse(sm.getProgress("level_3").unlocked)
    }

    // ---- 通关解锁 ----

    @Test
    fun `clearing level_1 unlocks level_2 and records best`() {
        val next = sm.onLevelCleared("level_1", ClearStats(timeMs = 60_000, sunLeft = 120))
        assertEquals("level_2", next)

        val p1 = sm.getProgress("level_1")
        assertTrue(p1.cleared)
        assertEquals(60_000L, p1.bestTimeMs)
        assertEquals(120, p1.bestSunLeft)

        val p2 = sm.getProgress("level_2")
        assertTrue(p2.unlocked)
        assertFalse(p2.cleared)
    }

    @Test
    fun `clearing last level returns null as next level`() {
        sm.onLevelCleared("level_1", ClearStats(60_000, 50))
        sm.onLevelCleared("level_2", ClearStats(120_000, 40))
        val next = sm.onLevelCleared("level_3", ClearStats(180_000, 30))
        assertNull(next)
        assertTrue(sm.getProgress("level_3").cleared)
    }

    // ---- 最佳纪录覆盖 ----

    @Test
    fun `better time overrides previous best time`() {
        sm.onLevelCleared("level_1", ClearStats(80_000, 50))
        sm.onLevelCleared("level_1", ClearStats(60_000, 30)) // time↓ 更好；sun↓ 更差
        val p = sm.getProgress("level_1")
        assertEquals(60_000L, p.bestTimeMs) // 被覆盖
        assertEquals(50, p.bestSunLeft)     // 保留之前更高的
    }

    @Test
    fun `worse time keeps previous best`() {
        sm.onLevelCleared("level_1", ClearStats(60_000, 100))
        sm.onLevelCleared("level_1", ClearStats(90_000, 120))
        val p = sm.getProgress("level_1")
        assertEquals(60_000L, p.bestTimeMs)
        assertEquals(120, p.bestSunLeft) // sun 更优
    }

    // ---- 统计 ----

    @Test
    fun `addKills accumulates across calls`() {
        sm.addKills(10)
        sm.addKills(5)
        assertEquals(15L, sm.getTotalKills())
    }

    @Test
    fun `addPlanted accumulates`() {
        sm.addPlanted(3)
        sm.addPlanted(7)
        assertEquals(10L, sm.getTotalPlanted())
    }

    @Test
    fun `addKills with 0 or negative is ignored`() {
        sm.addKills(0)
        sm.addKills(-1)
        assertEquals(0L, sm.getTotalKills())
    }

    // ---- 静音 ----

    @Test
    fun `mute default false then togglable`() {
        assertFalse(sm.isMuted())
        sm.setMuted(true)
        assertTrue(sm.isMuted())
        sm.setMuted(false)
        assertFalse(sm.isMuted())
    }

    // ---- 清空 ----

    @Test
    fun `clearAll resets progress and stats`() {
        sm.onLevelCleared("level_1", ClearStats(60_000, 50))
        sm.addKills(100)
        sm.setMuted(true)

        sm.clearAll()

        assertTrue(sm.getProgress("level_1").unlocked) // 默认仍解锁
        assertFalse(sm.getProgress("level_1").cleared)
        assertFalse(sm.getProgress("level_2").unlocked)
        assertEquals(0L, sm.getTotalKills())
        assertFalse(sm.isMuted())
    }

    // ---- 持久化回读 ----

    @Test
    fun `progress survives SaveManager recreation with same prefs`() {
        sm.onLevelCleared("level_1", ClearStats(45_000, 200))
        val sm2 = SaveManager.from(prefs)
        val p = sm2.getProgress("level_1")
        assertTrue(p.cleared)
        assertEquals(45_000L, p.bestTimeMs)
        assertEquals(200, p.bestSunLeft)
    }

    // ---- getAllProgress 顺序与数量 ----

    @Test
    fun `getAllProgress returns 3 entries in order`() {
        val all = sm.getAllProgress()
        assertEquals(3, all.size)
        assertEquals("level_1", all[0].levelId)
        assertEquals("level_2", all[1].levelId)
        assertEquals("level_3", all[2].levelId)
    }
}
