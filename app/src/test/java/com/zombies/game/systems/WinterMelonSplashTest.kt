package com.zombies.game.systems

import com.zombies.game.components.Health
import com.zombies.game.components.StatusEffects
import com.zombies.game.components.Transform
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.ProjectileFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 西瓜投手（wintermelon）子弹命中测试：
 *  - 主目标吃全额伤害 + slow
 *  - 前后 splashRadius 内的同行其他僵尸吃 ratio×damage（不含 slow）
 *  - 非同行不受影响
 */
class WinterMelonSplashTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun melonHitsPrimaryWithSlow_andSplashesRowNeighbors() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileSystem())
        world.addSystem(ProjectileCollisionSystem(ctx))

        val primary = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        val neighbor = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        val offRow = ZombieFactory.create(world, ZombieConfig.BASIC, row = 3)
        world.update(0L)

        primary.get<Transform>()!!.x = 800f
        neighbor.get<Transform>()!!.x = 880f // 同行、距离主目标 80（< splashRadius=120）
        offRow.get<Transform>()!!.x = 800f   // 不同行

        ProjectileFactory.createMelon(
            world,
            row = 2,
            startX = 800f,
            startY = primary.get<Transform>()!!.y,
            speed = 0f,
            damage = 60,
            slowMultiplier = 0.5f,
            slowDurationMs = 2000L,
            splashRadius = 120f,
            splashDamageRatio = 0.3f
        )
        world.update(16L)

        // 主目标：吃全额 60 伤
        val basicHp = ZombieConfig.BASIC.hp
        assertEquals(basicHp - 60, primary.get<Health>()!!.hp)
        // 主目标：被 slow
        assertEquals(0.5f, primary.get<StatusEffects>()!!.currentSlowMultiplier(), 0.0001f)
        // 邻居：吃 18 溅射伤（60 * 0.3 = 18）
        assertEquals(basicHp - 18, neighbor.get<Health>()!!.hp)
        // 邻居：不被 slow
        assertEquals(1f, neighbor.get<StatusEffects>()!!.currentSlowMultiplier(), 0.0001f)
        // 异行：完全不受影响
        assertEquals(basicHp, offRow.get<Health>()!!.hp)
    }

    @Test
    fun melonSplashDoesNotReachOutsideRadius() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileSystem())
        world.addSystem(ProjectileCollisionSystem(ctx))

        val primary = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        val far = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)

        primary.get<Transform>()!!.x = 800f
        far.get<Transform>()!!.x = 1000f // 距离 200 > splashRadius=120

        ProjectileFactory.createMelon(
            world, row = 2,
            startX = 800f, startY = primary.get<Transform>()!!.y,
            speed = 0f, damage = 60,
            slowMultiplier = 0.5f, slowDurationMs = 2000L,
            splashRadius = 120f, splashDamageRatio = 0.3f
        )
        world.update(16L)

        assertEquals(ZombieConfig.BASIC.hp - 60, primary.get<Health>()!!.hp)
        assertEquals(ZombieConfig.BASIC.hp, far.get<Health>()!!.hp)
    }

    @Test
    fun jesterReflectDoesNotReflectMelon() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileSystem())
        world.addSystem(ProjectileCollisionSystem(ctx))

        val jester = ZombieFactory.create(world, ZombieConfig.JESTER, row = 2)
        world.update(0L)
        jester.get<Transform>()!!.x = 800f

        ProjectileFactory.createMelon(
            world, row = 2,
            startX = 800f, startY = jester.get<Transform>()!!.y,
            speed = 0f, damage = 60,
            slowMultiplier = 0.5f, slowDurationMs = 2000L,
            splashRadius = 120f, splashDamageRatio = 0.3f
        )
        world.update(16L)

        // Jester 吃了全额伤害（溅射豆无视反弹盾）
        assertTrue(jester.get<Health>()!!.hp < ZombieConfig.JESTER.hp)
    }
}
