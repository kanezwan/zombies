package com.zombies.game.systems

import com.zombies.game.components.Health
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.PlantFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthSystemKillCountTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun zombieDeathIncrementsKillCount() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(HealthSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.BASIC, row = 0)
        world.update(0L)
        assertEquals(0, ctx.killsThisRun)

        // 扣血至死
        z.require<Health>().damage(9999)
        world.update(16L)

        assertEquals(1, ctx.killsThisRun)
        assertEquals(null, world.getEntity(z.id))
    }

    @Test
    fun plantDeathDoesNotIncrementKillCount() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(HealthSystem(ctx))

        val p = PlantFactory.create(world, PlantConfig.SUNFLOWER, row = 0, col = 0)
        world.update(0L)

        p.require<Health>().damage(9999)
        world.update(16L)

        assertEquals(0, ctx.killsThisRun)
    }

    @Test
    fun multipleZombieKillsAccumulate() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(HealthSystem(ctx))

        val zs = (0 until 3).map { ZombieFactory.create(world, ZombieConfig.BASIC, row = 0) }
        world.update(0L)
        zs.forEach { it.require<Health>().damage(9999) }
        world.update(16L)

        assertEquals(3, ctx.killsThisRun)
    }

    @Test
    fun resetClearsKillCount() {
        val ctx = newCtx()
        ctx.killsThisRun = 5
        ctx.plantedThisRun = 7
        ctx.reset()
        assertEquals(0, ctx.killsThisRun)
        assertEquals(0, ctx.plantedThisRun)
    }
}
