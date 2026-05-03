package com.zombies.game.config

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigLoaderTest {

    @Test
    fun parsesSunflower() {
        val json = """
            {
              "sunflower": {
                "cost": 50,
                "cooldownMs": 7500,
                "hp": 300,
                "produceIntervalMs": 24000,
                "produceAmount": 25
              }
            }
        """.trimIndent()
        val map = ConfigLoader.parsePlants(json)
        val sf = map.getValue("sunflower")
        assertEquals("sunflower", sf.type)
        assertEquals(50, sf.cost)
        assertEquals(7500L, sf.cooldownMs)
        assertEquals(300, sf.hp)
        assertEquals(24000L, sf.produceIntervalMs)
        assertEquals(25, sf.produceAmount)
    }

    @Test
    fun parsesPeashooter() {
        val json = """
            {
              "peashooter": {
                "cost": 100,
                "cooldownMs": 7500,
                "hp": 300,
                "attackIntervalMs": 1500,
                "damage": 20,
                "projectileSpeed": 520
              }
            }
        """.trimIndent()
        val map = ConfigLoader.parsePlants(json)
        val p = map.getValue("peashooter")
        assertEquals(100, p.cost)
        assertEquals(1500L, p.attackIntervalMs)
        assertEquals(20, p.damage)
        assertEquals(520f, p.projectileSpeed, 0.001f)
    }

    @Test
    fun missingFieldsGetDefaults() {
        val json = """{"mystery": {"cost": 25}}"""
        val map = ConfigLoader.parsePlants(json)
        val m = map.getValue("mystery")
        assertEquals(25, m.cost)
        assertEquals(0L, m.cooldownMs)
        assertEquals(100, m.hp)
        assertEquals(0, m.damage)
        assertEquals(false, m.polevaultImmune)
        assertEquals(1f, m.torchMultiplier, 0.001f)
        assertEquals("", m.spriteKey)
    }

    @Test
    fun parsesTallNutAndTorchwoodTraits() {
        val json = """
            {
              "tallnut": {"cost": 125, "cooldownMs": 30000, "hp": 8000, "polevaultImmune": true},
              "torchwood": {"cost": 175, "cooldownMs": 7500, "hp": 300, "torchMultiplier": 2.0, "spriteKey": "sprites/plants/torchwood"}
            }
        """.trimIndent()
        val map = ConfigLoader.parsePlants(json)
        val tn = map.getValue("tallnut")
        assertEquals(8000, tn.hp)
        assertEquals(true, tn.polevaultImmune)

        val tw = map.getValue("torchwood")
        assertEquals(2f, tw.torchMultiplier, 0.001f)
        assertEquals("sprites/plants/torchwood", tw.spriteKey)
    }

    @Test
    fun parsesPuffShroomNightOnly() {
        val json = """{"puffshroom": {"cost": 0, "hp": 300, "nightOnly": true}}"""
        val map = ConfigLoader.parsePlants(json)
        val p = map.getValue("puffshroom")
        assertEquals(true, p.nightOnly)
    }

    @Test
    fun parsesWinterMelonSplashAndSlow() {
        val json = """
            {
              "wintermelon": {
                "cost": 200, "cooldownMs": 7500, "hp": 300,
                "attackIntervalMs": 2000, "damage": 60, "projectileSpeed": 420,
                "slowMultiplier": 0.5, "slowDurationMs": 2000,
                "splashRadius": 120, "splashDamageRatio": 0.3
              }
            }
        """.trimIndent()
        val map = ConfigLoader.parsePlants(json)
        val w = map.getValue("wintermelon")
        assertEquals(200, w.cost)
        assertEquals(60, w.damage)
        assertEquals(0.5f, w.slowMultiplier, 0.001f)
        assertEquals(2000L, w.slowDurationMs)
        assertEquals(120f, w.splashRadius, 0.001f)
        assertEquals(0.3f, w.splashDamageRatio, 0.001f)
    }

    @Test
    fun parsesZombieNewspaperJesterFields() {
        val json = """
            {
              "newspaper": {"hp": 200, "speed": 18, "armorHp": 300, "enragedSpeedMultiplier": 2.5},
              "jester": {"hp": 500, "speed": 32, "reflectProjectiles": true}
            }
        """.trimIndent()
        val map = ConfigLoader.parseZombies(json)
        val n = map.getValue("newspaper")
        assertEquals(300, n.armorHp)
        assertEquals(2.5f, n.enragedSpeedMultiplier, 0.001f)
        assertEquals(false, n.reflectProjectiles)

        val j = map.getValue("jester")
        assertEquals(true, j.reflectProjectiles)
        assertEquals(0, j.armorHp)
    }
}
