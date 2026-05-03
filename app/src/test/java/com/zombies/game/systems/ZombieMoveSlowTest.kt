package com.zombies.game.systems

import com.zombies.game.components.StatusEffects
import com.zombies.game.components.Transform
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Test

class ZombieMoveSlowTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun slowEffectReducesZombieSpeed() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ZombieMoveSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)
        val t = z.get<Transform>()!!
        val startX = t.x

        // 无减速：1000ms 应推进 baseSpeed=22 像素
        world.update(1000L)
        val deltaNoSlow = startX - t.x
        assertEquals(22f, deltaNoSlow, 0.5f)

        // 施加 0.5 减速
        z.get<StatusEffects>()!!.addOrRefreshSlow("snowpea", 0.5f, 10_000L)
        val beforeSlow = t.x
        world.update(1000L)
        val deltaSlow = beforeSlow - t.x
        assertEquals(11f, deltaSlow, 0.5f)
    }
}
