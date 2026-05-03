package com.zombies.game.systems

import com.zombies.game.components.Boss
import com.zombies.game.components.Health
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.ZombieFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * M11: 验证 [BossHudRenderSystem.pickTargetBoss] 的筛选逻辑。
 *
 *  - 无 Boss：返回 null
 *  - 单个 Boss：返回之
 *  - 多个 Boss：返回 HP 最低的
 *  - 全部 isDead：返回 null
 */
class BossHudRenderSystemTest {

    @Test
    fun returnsNull_whenNoBossPresent() {
        val world = World()
        ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)

        val result = BossHudRenderSystem.pickTargetBoss(world)
        assertNull(result)
    }

    @Test
    fun returnsSingleBoss_whenOnePresent() {
        val world = World()
        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        val result = BossHudRenderSystem.pickTargetBoss(world)
        assertSame(boss.get<Health>(), result!!.health)
        assertSame(boss.get<Boss>(), result.boss)
    }

    @Test
    fun returnsLowestHpBoss_whenMultiplePresent() {
        val world = World()
        val bossA = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 0)
        val bossB = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        val bossC = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 4)
        world.update(0L)

        // bossB 最残血
        bossA.get<Health>()!!.hp = 6000
        bossB.get<Health>()!!.hp = 1000
        bossC.get<Health>()!!.hp = 4000

        val result = BossHudRenderSystem.pickTargetBoss(world)
        assertEquals(1000, result!!.health.hp)
        assertSame(bossB.get<Health>(), result.health)
    }

    @Test
    fun ignoresDeadBosses() {
        val world = World()
        val bossA = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 0)
        val bossB = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        bossA.get<Health>()!!.hp = 0   // 已死
        bossB.get<Health>()!!.hp = 5000

        val result = BossHudRenderSystem.pickTargetBoss(world)
        assertSame(bossB.get<Health>(), result!!.health)
    }

    @Test
    fun returnsNull_whenAllBossesDead() {
        val world = World()
        val bossA = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 0)
        val bossB = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        bossA.get<Health>()!!.hp = 0
        bossB.get<Health>()!!.hp = 0

        val result = BossHudRenderSystem.pickTargetBoss(world)
        assertNull(result)
    }

    @Test
    fun tracksBossPhaseOnReturnedComponent() {
        val world = World()
        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        boss.get<Boss>()!!.phase = 3

        val result = BossHudRenderSystem.pickTargetBoss(world)
        assertEquals(3, result!!.boss.phase)
    }
}
