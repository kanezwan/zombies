package com.zombies.game.systems

import com.zombies.game.components.Projectile
import com.zombies.game.components.ProjectileTag
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.PlantFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShooterSystemTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    private fun countProjectiles(world: World): Int {
        var n = 0
        world.forEachWith<Projectile> { _, _ -> n++ }
        return n
    }

    private fun countIceProjectiles(world: World): Int {
        var n = 0
        world.forEachWith<ProjectileTag> { _, tag -> if (tag.kind == "icepea") n++ }
        return n
    }

    @Test
    fun peashooterFiresSingleShotPerInterval() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ShooterSystem(shouldCheckZombie = false))
        PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 1)
        world.update(0L)

        // 刚好到 interval
        world.update(PlantConfig.PEASHOOTER.attackIntervalMs)
        assertEquals(1, countProjectiles(world))

        // 再一个 interval
        world.update(PlantConfig.PEASHOOTER.attackIntervalMs)
        assertEquals(2, countProjectiles(world))
    }

    @Test
    fun repeaterFiresTwoShotsPerInterval() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ShooterSystem(shouldCheckZombie = false))
        PlantFactory.create(world, PlantConfig.REPEATER, row = 2, col = 1)
        world.update(0L)

        // 到达 interval → 立即第一发
        world.update(PlantConfig.REPEATER.attackIntervalMs)
        assertTrue(countProjectiles(world) >= 1)

        // 推进足够时间让第二发排队到期
        world.update(PlantConfig.REPEATER.shotGapMs + 10L)
        assertEquals(2, countProjectiles(world))
    }

    @Test
    fun snowpeaProducesIceProjectileWithSlowParams() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ShooterSystem(shouldCheckZombie = false))
        PlantFactory.create(world, PlantConfig.SNOWPEA, row = 2, col = 1)
        world.update(0L)

        world.update(PlantConfig.SNOWPEA.attackIntervalMs)
        assertEquals(1, countIceProjectiles(world))

        var found: Projectile? = null
        world.forEachWith<Projectile> { _, p -> found = p }
        val proj = found!!
        assertTrue(proj.hasSlow())
        assertEquals(0.5f, proj.slowMultiplier, 0.0001f)
        assertEquals(3000L, proj.slowDurationMs)
        assertEquals("snowpea", proj.slowSource)
    }

    @Test
    fun shooterWaitsWhenNoZombieInRow() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ShooterSystem(shouldCheckZombie = true))
        PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 1)
        world.update(0L)

        // 同行无僵尸 → 不应开火
        world.update(5_000L)
        assertEquals(0, countProjectiles(world))

        // 同行放一只僵尸后再推进一个 interval，应开火
        ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L) // flush
        world.update(PlantConfig.PEASHOOTER.attackIntervalMs)
        assertTrue(countProjectiles(world) >= 1)
    }
}
