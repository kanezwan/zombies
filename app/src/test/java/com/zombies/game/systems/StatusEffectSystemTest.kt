package com.zombies.game.systems

import com.zombies.game.components.Renderable
import com.zombies.game.components.StatusEffects
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusEffectSystemTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun slowedZombieTurnsBlueAndRestoresAfterExpire() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(StatusEffectSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)

        z.get<StatusEffects>()!!.addOrRefreshSlow("snowpea", 0.5f, 500L)

        // 立即 tick 一下，应变蓝
        world.update(16L)
        assertEquals(Renderable.ZOMBIE_SLOWED, z.get<Renderable>()!!.color)

        // 推进到过期
        world.update(600L)
        assertEquals(Renderable.ZOMBIE_BASIC, z.get<Renderable>()!!.color)
    }

    @Test
    fun coneheadSlowedStillRestoresToConeColor() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(StatusEffectSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.CONEHEAD, row = 1)
        world.update(0L)

        z.get<StatusEffects>()!!.addOrRefreshSlow("snowpea", 0.5f, 200L)
        world.update(16L)
        assertEquals(Renderable.ZOMBIE_SLOWED, z.get<Renderable>()!!.color)

        world.update(400L)
        assertEquals(Renderable.ZOMBIE_CONEHEAD, z.get<Renderable>()!!.color)
    }
}
