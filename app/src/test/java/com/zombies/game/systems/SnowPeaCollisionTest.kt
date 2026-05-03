package com.zombies.game.systems

import com.zombies.game.components.Health
import com.zombies.game.components.StatusEffects
import com.zombies.game.components.Transform
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.PlantFactory
import com.zombies.game.entity.ProjectileFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnowPeaCollisionTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun icePeaHitAppliesSlow() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileSystem())
        world.addSystem(ProjectileCollisionSystem())

        val z = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)
        val zt = z.get<Transform>()!!
        zt.x = 800f
        // 冰豆子精准命中位置
        ProjectileFactory.createIcePea(
            world,
            row = 2,
            startX = 800f,
            startY = zt.y,
            speed = 0f,
            damage = 10,
            slowMultiplier = 0.5f,
            slowDurationMs = 3000L,
            slowSource = "snowpea"
        )
        world.update(16L)

        val effects = z.get<StatusEffects>()!!
        assertEquals(0.5f, effects.currentSlowMultiplier(), 0.0001f)
        // 伤害生效
        assertTrue(z.get<Health>()!!.hp < ZombieConfig.BASIC.hp)
    }

    @Test
    fun normalPeaDoesNotApplySlow() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileSystem())
        world.addSystem(ProjectileCollisionSystem())

        val z = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)
        val zt = z.get<Transform>()!!
        zt.x = 800f
        ProjectileFactory.createPea(
            world, row = 2, startX = 800f, startY = zt.y, speed = 0f, damage = 10
        )
        world.update(16L)

        val effects = z.get<StatusEffects>()!!
        assertEquals(1f, effects.currentSlowMultiplier(), 0.0001f)
    }
}
