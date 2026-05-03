package com.zombies.game.systems

import com.zombies.game.components.Transform
import com.zombies.game.components.Velocity
import com.zombies.game.components.ZombieMover
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZombieMoveSystemTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun walkingAdvancesLeftward() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ZombieMoveSystem(ctx))
        val e = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L) // 让 pendingAdd 刷新
        val t = e.get<Transform>()!!
        val x0 = t.x
        world.update(1_000L) // 1s
        val x1 = t.x
        // 速度为 22 px/s，x 应该往左移动 ~22
        assertTrue("should move left", x1 < x0)
        assertEquals(22f, x0 - x1, 0.5f)
    }

    @Test
    fun eatingStopsMovement() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ZombieMoveSystem(ctx))
        val e = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)
        e.get<ZombieMover>()!!.state = ZombieMover.State.EATING
        val x0 = e.get<Transform>()!!.x
        world.update(1_000L)
        val x1 = e.get<Transform>()!!.x
        assertEquals(x0, x1, 0.0001f)
        // 速度被置 0
        assertEquals(0f, e.get<Velocity>()!!.vx, 0.0001f)
    }

    @Test
    fun pausedWhenStateNotRunning() {
        val ctx = newCtx()
        ctx.state = GameContext.State.VICTORY
        val world = World()
        world.addSystem(ZombieMoveSystem(ctx))
        val e = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)
        val x0 = e.get<Transform>()!!.x
        world.update(2_000L)
        val x1 = e.get<Transform>()!!.x
        assertEquals(x0, x1, 0.0001f)
    }
}
