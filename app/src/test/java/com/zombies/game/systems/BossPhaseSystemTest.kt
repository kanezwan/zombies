package com.zombies.game.systems

import com.zombies.game.components.Boss
import com.zombies.game.components.Health
import com.zombies.game.components.PlantFreeze
import com.zombies.game.components.PlantTag
import com.zombies.game.components.Renderable
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.PlantFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M11: 验证 [BossAISystem] 的阶段状态机。
 *
 * 规则：
 *  - HP > 66%：phase=1
 *  - HP ≤ 66%：phase=2（一次性进入，并加速下次召唤）
 *  - HP ≤ 33%：phase=3（一次性进入，并冻结全场植物 5s）
 *  - 阶段只单向递增，不回退
 */
class BossPhaseSystemTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun bossStartsAtPhase1_whenFullHp() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(BossAISystem(ctx))
        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        world.update(16L)
        assertEquals(1, boss.get<Boss>()!!.phase)
    }

    @Test
    fun bossEntersPhase2_whenHpReaches66Percent() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(BossAISystem(ctx))
        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        // 把 HP 打到 60% —— 应进入 phase 2
        val hp = boss.get<Health>()!!
        hp.hp = (hp.maxHp * 0.60f).toInt()

        world.update(16L)

        val bComp = boss.get<Boss>()!!
        assertEquals(2, bComp.phase)
        // phase 2 进入时召唤冷却应被减半（<= 完整间隔 / 2，且 >= 1000）
        assertTrue(
            "summonCooldown should be halved on phase 2: ${bComp.summonCooldownMs}",
            bComp.summonCooldownMs <= bComp.summonIntervalMs / 2
        )
    }

    @Test
    fun bossEntersPhase3_whenHpReaches33Percent_andFreezesAllPlants() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(BossAISystem(ctx))
        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        // 预置若干植物
        val p1 = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 0, col = 1)
        val p2 = PlantFactory.create(world, PlantConfig.SUNFLOWER, row = 3, col = 4)
        world.update(0L)

        // HP 一次性拉到 30% —— 跳过 phase 2 直接到 phase 3？
        // 规则是 ratio <= 0.33 → phase 3；但若当前还是 phase 1 也会直接 newPhase=3 一次性切到 3。
        val hp = boss.get<Health>()!!
        hp.hp = (hp.maxHp * 0.30f).toInt()

        world.update(16L)

        val bComp = boss.get<Boss>()!!
        assertEquals(3, bComp.phase)
        assertTrue("freezeAllTriggered should be set", bComp.freezeAllTriggered)

        // 两株植物都应被挂上 PlantFreeze
        assertNotNull("p1 should be frozen", p1.get<PlantFreeze>())
        assertNotNull("p2 should be frozen", p2.get<PlantFreeze>())
        assertEquals(5_000L, p1.get<PlantFreeze>()!!.remainingMs)
    }

    @Test
    fun bossFreezeAllOnlyTriggersOnce_evenIfHpBelow33ForMultipleFrames() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(BossAISystem(ctx))
        world.addSystem(PlantFreezeSystem())
        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        val plant = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 1)
        world.update(0L)

        val hp = boss.get<Health>()!!
        hp.hp = (hp.maxHp * 0.20f).toInt()

        // 第一帧：触发冻结 5000ms
        world.update(16L)
        val frozen1 = plant.get<PlantFreeze>()
        assertNotNull(frozen1)

        // 第二帧：仍在 phase 3 且 HP 继续 <= 33%，freezeAllTriggered 幂等，
        // 不应再次 "刷新" 冻结到 5000ms（以 PlantFreezeSystem 的倒计时为准）
        world.update(200L)
        val frozen2 = plant.get<PlantFreeze>()
        assertNotNull(frozen2)
        assertTrue(
            "freeze should keep counting down, not be re-refreshed: ${frozen2!!.remainingMs}",
            frozen2.remainingMs < 5_000L
        )

        assertTrue(boss.get<Boss>()!!.freezeAllTriggered)
    }

    @Test
    fun bossPhaseDoesNotRegress_whenHpIsHealedAboveThreshold() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(BossAISystem(ctx))
        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        val hp = boss.get<Health>()!!
        // 先打到 phase 3
        hp.hp = (hp.maxHp * 0.20f).toInt()
        world.update(16L)
        assertEquals(3, boss.get<Boss>()!!.phase)

        // 恢复到满血 —— 阶段不应退回
        hp.hp = hp.maxHp
        world.update(16L)
        assertEquals(3, boss.get<Boss>()!!.phase)
    }

    @Test
    fun bossRenderColorSwitches_perPhase() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(BossAISystem(ctx))
        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        // phase 1
        val hp = boss.get<Health>()!!
        world.update(16L)
        assertEquals(Renderable.ZOMBIE_BOSS, boss.get<Renderable>()!!.color)

        // phase 2
        hp.hp = (hp.maxHp * 0.60f).toInt()
        world.update(16L)
        assertEquals(Renderable.ZOMBIE_BOSS_PHASE2, boss.get<Renderable>()!!.color)

        // phase 3
        hp.hp = (hp.maxHp * 0.20f).toInt()
        world.update(16L)
        assertEquals(Renderable.ZOMBIE_BOSS_PHASE3, boss.get<Renderable>()!!.color)
    }
}
