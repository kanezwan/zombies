package com.zombies.game.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 植物 & 僵尸配置验证（累积覆盖 M5 + M6）：
 *  - 植物：sunflower / peashooter / repeater / snowpea / wallnut / cherrybomb
 *  - 僵尸：basic / conehead / buckethead / flag / polevault
 */
class PlantZombieConfigTest {

    @Test
    fun plantDefaultsContainsAllTypes() {
        val keys = PlantConfig.DEFAULTS.map { it.type }.toSet()
        assertTrue(keys.contains("sunflower"))
        assertTrue(keys.contains("peashooter"))
        assertTrue(keys.contains("repeater"))
        assertTrue(keys.contains("snowpea"))
        assertTrue(keys.contains("wallnut"))
        assertTrue(keys.contains("cherrybomb"))
        assertEquals(6, PlantConfig.DEFAULTS.size)
    }

    @Test
    fun zombieDefaultsContainsAllTypes() {
        val keys = ZombieConfig.DEFAULTS.map { it.type }.toSet()
        assertTrue(keys.contains("basic"))
        assertTrue(keys.contains("conehead"))
        assertTrue(keys.contains("buckethead"))
        assertTrue(keys.contains("flag"))
        assertTrue(keys.contains("polevault"))
    }

    @Test
    fun coneheadHpIsGreaterThanBasic() {
        assertTrue(ZombieConfig.CONEHEAD.hp > ZombieConfig.BASIC.hp)
        assertTrue(ZombieConfig.BUCKETHEAD.hp > ZombieConfig.CONEHEAD.hp)
    }

    @Test
    fun cherryBombHasExplodeDamageAndFuse() {
        assertTrue(PlantConfig.CHERRY_BOMB.fuseMs > 0)
        assertTrue(PlantConfig.CHERRY_BOMB.explodeDamage > 0)
        assertTrue(PlantConfig.CHERRY_BOMB.explodeRadius > 0f)
    }

    @Test
    fun wallnutHasHighHpAndNoAttack() {
        assertTrue(PlantConfig.WALLNUT.hp >= 2000)
        assertEquals(0, PlantConfig.WALLNUT.damage)
        assertEquals(0, PlantConfig.WALLNUT.produceAmount)
    }

    @Test
    fun repeaterShootsTwoAndSnowPeaHasSlow() {
        assertEquals(2, PlantConfig.REPEATER.shotsPerFire)
        assertTrue(PlantConfig.REPEATER.shotGapMs > 0)

        assertTrue(PlantConfig.SNOWPEA.slowMultiplier < 1f)
        assertTrue(PlantConfig.SNOWPEA.slowMultiplier > 0f)
        assertTrue(PlantConfig.SNOWPEA.slowDurationMs > 0)
    }

    @Test
    fun flagAndPolevaultSpecials() {
        assertTrue(ZombieConfig.FLAG.speed > ZombieConfig.BASIC.speed)
        assertTrue(ZombieConfig.POLEVAULT.jumpDistance > 0f)
        assertTrue(ZombieConfig.POLEVAULT.speed > ZombieConfig.BASIC.speed)
    }

    @Test
    fun parsePlantsWithM6Fields() {
        val json = """
            {
              "cherrybomb": {
                "cost": 150,
                "cooldownMs": 50000,
                "hp": 300,
                "fuseMs": 1200,
                "explodeRadius": 260,
                "explodeDamage": 1800
              },
              "wallnut": {
                "cost": 50,
                "cooldownMs": 30000,
                "hp": 4000
              },
              "repeater": {
                "cost": 200,
                "cooldownMs": 7500,
                "hp": 300,
                "attackIntervalMs": 1500,
                "damage": 20,
                "projectileSpeed": 520,
                "shotsPerFire": 2,
                "shotGapMs": 140
              },
              "snowpea": {
                "cost": 175,
                "cooldownMs": 7500,
                "hp": 300,
                "attackIntervalMs": 1500,
                "damage": 20,
                "projectileSpeed": 520,
                "slowMultiplier": 0.5,
                "slowDurationMs": 3000
              }
            }
        """.trimIndent()
        val map = ConfigLoader.parsePlants(json)
        val cb = map["cherrybomb"]!!
        assertEquals(1200L, cb.fuseMs)
        assertEquals(1800, cb.explodeDamage)
        assertEquals(260f, cb.explodeRadius, 0.01f)
        val wn = map["wallnut"]!!
        assertEquals(4000, wn.hp)
        assertEquals(0L, wn.fuseMs)
        val rep = map["repeater"]!!
        assertEquals(2, rep.shotsPerFire)
        assertEquals(140L, rep.shotGapMs)
        val sp = map["snowpea"]!!
        assertEquals(0.5f, sp.slowMultiplier, 0.0001f)
        assertEquals(3000L, sp.slowDurationMs)
    }

    @Test
    fun parseZombiesWithM6Types() {
        val json = """
            {
              "conehead": { "hp": 570, "speed": 22, "attackDamage": 20, "attackIntervalMs": 1000 },
              "buckethead": { "hp": 1300, "speed": 22, "attackDamage": 20, "attackIntervalMs": 1000 },
              "flag": { "hp": 200, "speed": 28, "attackDamage": 20, "attackIntervalMs": 1000 },
              "polevault": { "hp": 340, "speed": 46, "attackDamage": 20, "attackIntervalMs": 1000, "jumpDistance": 180 }
            }
        """.trimIndent()
        val map = ConfigLoader.parseZombies(json)
        assertEquals(570, map["conehead"]!!.hp)
        assertEquals(1300, map["buckethead"]!!.hp)
        assertEquals(28f, map["flag"]!!.speed, 0.0001f)
        assertEquals(180f, map["polevault"]!!.jumpDistance, 0.0001f)
    }
}
