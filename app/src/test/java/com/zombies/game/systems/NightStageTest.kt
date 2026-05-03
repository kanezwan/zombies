package com.zombies.game.systems

import com.zombies.game.components.PlantTag
import com.zombies.game.components.Sleep
import com.zombies.game.components.Sun
import com.zombies.game.components.Transform
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.StageConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.PlantFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 夜间场景 / 夜间植物相关测试。
 */
class NightStageTest {

    private fun ctxOf(stage: StageConfig) = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        stage = stage
    )

    @Test
    fun nightStageDisablesSkySunSpawn() {
        val ctx = ctxOf(StageConfig.NIGHT)
        val world = World()
        val sys = SunSpawnSystem(virtualWidth = 1920f, intervalMs = 100, firstDelayMs = 0, ctx = ctx)
        world.addSystem(sys)

        world.update(500) // 远超 intervalMs × 多次
        var n = 0
        world.forEachWith<Sun> { _, _ -> n++ }
        assertEquals("Night: no sky sun", 0, n)
    }

    @Test
    fun dayStageSpawnsSkySun() {
        val ctx = ctxOf(StageConfig.DAY)
        val world = World()
        val sys = SunSpawnSystem(virtualWidth = 1920f, intervalMs = 100, firstDelayMs = 0, ctx = ctx)
        world.addSystem(sys)

        world.update(250) // 多次触发
        var n = 0
        world.forEachWith<Sun> { _, _ -> n++ }
        assertTrue("Day: at least one sun", n >= 1)
    }

    @Test
    fun puffShroomSleepsInDay() {
        val ctx = ctxOf(StageConfig.DAY)
        val world = World()
        world.addSystem(NightPlantSleepSystem(ctx))
        val e = PlantFactory.create(world, PlantConfig.PUFFSHROOM, row = 0, col = 0)
        world.update(0L) // 触发 pending
        world.update(16L)
        assertTrue("Night plant should sleep in day", e.get<Sleep>()!!.asleep)
    }

    @Test
    fun puffShroomAwakesInNight() {
        val ctx = ctxOf(StageConfig.NIGHT)
        val world = World()
        world.addSystem(NightPlantSleepSystem(ctx))
        val e = PlantFactory.create(world, PlantConfig.PUFFSHROOM, row = 0, col = 0)
        world.update(0L)
        world.update(16L)
        assertFalse("Night plant should be awake at night", e.get<Sleep>()!!.asleep)
    }

    @Test
    fun availablePlantsFiltersNightOnlyInDay() {
        val ctx = ctxOf(StageConfig.DAY)
        val hasPuff = ctx.availablePlants().any { it.type == "puffshroom" }
        assertFalse("Day: should hide night-only plants", hasPuff)
    }

    @Test
    fun availablePlantsIncludesNightOnlyAtNight() {
        val ctx = ctxOf(StageConfig.NIGHT)
        val hasPuff = ctx.availablePlants().any { it.type == "puffshroom" }
        assertTrue("Night: should include night-only plants", hasPuff)
    }

    @Test
    fun puffshroomShooterSkipsWhenAsleep() {
        val ctx = ctxOf(StageConfig.DAY)
        val world = World()
        world.addSystem(NightPlantSleepSystem(ctx))
        world.addSystem(ShooterSystem(shouldCheckZombie = false))
        val e = PlantFactory.create(world, PlantConfig.PUFFSHROOM, row = 0, col = 0)
        world.update(0L)
        world.update(16L) // 触发 Sleep=true

        // 推进足以触发射击的时间，但睡眠中应不产子弹
        repeat(100) { world.update(100L) }
        var bullets = 0
        world.forEachWith2<com.zombies.game.components.Projectile, Transform> { _, _, _ -> bullets++ }
        assertEquals("Sleeping puffshroom fires nothing", 0, bullets)
    }
}
