package com.zombies.game.systems

import com.zombies.game.components.Boss
import com.zombies.game.components.ZombieTag
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M11: 验证 Boss 召唤小怪技能。
 *
 *  - 冷却推进：[Boss.summonCooldownMs] 初始 15000，dt 累加后应归零触发召唤
 *  - 每次触发召唤 2 只小怪（basic / conehead），并加入 world
 *  - 非 Boss 僵尸数量递增 2
 */
class BossSummonSystemTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    private fun countNonBossZombies(world: World): Int {
        var n = 0
        world.forEachWith<ZombieTag> { e, _ ->
            if (e.get<Boss>() == null) n++
        }
        return n
    }

    @Test
    fun summonTriggersOnce_whenCooldownElapses() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(BossAISystem(ctx, random = Random(42)))
        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        val before = countNonBossZombies(world)

        // 一帧把整个 summon 间隔推进过去
        world.update(15_000L)
        // 再 flush 一帧让 pendingAdd 生效
        world.update(0L)

        val after = countNonBossZombies(world)
        assertEquals("summon should spawn exactly 2 minions", before + 2, after)

        // 冷却被重置回整段间隔（boss 组件内部）
        val bComp = boss.get<Boss>()!!
        assertEquals(bComp.summonIntervalMs, bComp.summonCooldownMs)
    }

    @Test
    fun summonDoesNotTrigger_beforeCooldownElapses() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(BossAISystem(ctx, random = Random(42)))
        ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        val before = countNonBossZombies(world)
        // 只走 14 秒（不足 15 秒召唤间隔）
        world.update(14_000L)
        world.update(0L)
        assertEquals(before, countNonBossZombies(world))
    }

    @Test
    fun multipleCooldownsTriggerMultipleSummons() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(BossAISystem(ctx, random = Random(123)))
        ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        val before = countNonBossZombies(world)

        // 分三次触发：每次 15 秒
        repeat(3) {
            world.update(15_000L)
            world.update(0L)
        }

        // 至少应该多了 3 * 2 = 6 只小怪（扔车不影响召唤计数）
        val after = countNonBossZombies(world)
        assertTrue(
            "expected >= 6 new minions, got ${after - before}",
            after - before >= 6
        )
    }

    @Test
    fun summonedMinionsAreBasicOrConehead() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(BossAISystem(ctx, random = Random(7)))
        ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        world.update(15_000L)
        world.update(0L)

        val allowed = setOf("basic", "conehead")
        world.forEachWith<ZombieTag> { e, tag ->
            if (e.get<Boss>() != null) return@forEachWith
            assertTrue(
                "minion type should be basic/conehead but was ${tag.type}",
                tag.type in allowed
            )
        }
    }
}
