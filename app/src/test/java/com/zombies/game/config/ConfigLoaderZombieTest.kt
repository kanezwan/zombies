package com.zombies.game.config

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigLoaderZombieTest {

    @Test
    fun parsesBasicZombie() {
        val json = """
            {
              "basic": {
                "hp": 250,
                "speed": 30,
                "attackDamage": 25,
                "attackIntervalMs": 900
              }
            }
        """.trimIndent()
        val map = ConfigLoader.parseZombies(json)
        val z = map.getValue("basic")
        assertEquals("basic", z.type)
        assertEquals(250, z.hp)
        assertEquals(30f, z.speed, 0.001f)
        assertEquals(25, z.attackDamage)
        assertEquals(900L, z.attackIntervalMs)
    }

    @Test
    fun parsesWaveAndSortsByTime() {
        val json = """
            {
              "levelId": "L1",
              "totalDurationMs": 60000,
              "entries": [
                { "atMs": 5000, "zombieType": "basic", "row": 2 },
                { "atMs": 1000, "zombieType": "basic", "row": -1 },
                { "atMs": 3000, "zombieType": "basic", "row": 0 }
              ]
            }
        """.trimIndent()
        val wave = ConfigLoader.parseWave(json)
        assertEquals("L1", wave.levelId)
        assertEquals(60000L, wave.totalDurationMs)
        assertEquals(3, wave.entries.size)
        assertEquals(1000L, wave.entries[0].atMs)
        assertEquals(3000L, wave.entries[1].atMs)
        assertEquals(5000L, wave.entries[2].atMs)
    }
}
