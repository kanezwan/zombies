package com.zombies.game.systems

import com.zombies.game.components.Health
import com.zombies.game.components.Projectile
import com.zombies.game.components.ReflectShield
import com.zombies.game.components.Transform
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.ProjectileFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 小丑 Jester 反弹测试。
 */
class JesterReflectTest {

    private fun newCtx() = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun plainPeaReflectsAndShieldDeactivates() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileCollisionSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.JESTER, row = 0)
        world.update(0L)
        val shield = z.get<ReflectShield>()!!
        assertTrue(shield.active)
        val hpBefore = z.get<Health>()!!.hp
        val zx = z.get<Transform>()!!.x

        val bullet = ProjectileFactory.createPea(world, row = 0, startX = zx, startY = 0f, speed = 0f, damage = 40)
        world.update(0L)
        world.update(16L)

        // 僵尸不掉血；盾失效；子弹仍然存在（位置已被推到 maxX 外，下一帧被 ProjectileSystem 清理）
        assertEquals(hpBefore, z.get<Health>()!!.hp)
        assertFalse(shield.active)
        assertNotNull(world.getEntity(bullet.id)) // 反弹后不立刻销毁
    }

    @Test
    fun icePeaIgnoresShieldAndDamagesJester() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileCollisionSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.JESTER, row = 0)
        world.update(0L)
        val hpBefore = z.get<Health>()!!.hp
        val zx = z.get<Transform>()!!.x

        ProjectileFactory.createIcePea(
            world, row = 0, startX = zx, startY = 0f, speed = 0f, damage = 40,
            slowMultiplier = 0.5f, slowDurationMs = 3000L, slowSource = "snowpea"
        )
        world.update(0L)
        world.update(16L)

        // 冰豆命中：扣血并附加减速（此处只验证扣血，减速由其他测试覆盖）
        assertEquals(hpBefore - 40, z.get<Health>()!!.hp)
    }

    @Test
    fun secondShotAfterReflectDoesDamageNormally() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileCollisionSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.JESTER, row = 0)
        world.update(0L)
        val zx = z.get<Transform>()!!.x

        // 第一发被反弹
        ProjectileFactory.createPea(world, row = 0, startX = zx, startY = 0f, speed = 0f, damage = 40)
        world.update(0L)
        world.update(16L)
        val hpAfter1 = z.get<Health>()!!.hp

        // 第二发正常命中
        val p2 = world.createEntity()
        p2.add(Transform(zx, 0f))
        p2.add(Projectile(row = 0, damage = 40, maxX = 99999f))
        world.update(0L)
        world.update(16L)
        val hpAfter2 = z.get<Health>()!!.hp

        assertEquals(hpAfter1 - 40, hpAfter2)
    }
}
