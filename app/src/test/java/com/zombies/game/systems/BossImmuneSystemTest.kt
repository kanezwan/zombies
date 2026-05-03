package com.zombies.game.systems

import com.zombies.game.components.Renderable
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
 * M11: 验证 Boss 对 slow/freeze 的免疫（两层防御）。
 *
 * 层 1：[ProjectileCollisionSystem] 命中 Boss 时不添加 slow 效果
 * 层 2：[StatusEffectSystem] 即便 Boss 上有 slow 效果（人为注入），也不覆盖 Boss 的渲染色
 */
class BossImmuneSystemTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun icePeaDoesNotSlowBoss() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileCollisionSystem(ctx))

        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)
        val bx = boss.get<Transform>()!!.x

        // 发射一颗冰豆直接命中 Boss
        ProjectileFactory.createIcePea(
            world = world, row = 2,
            startX = bx, startY = 0f,
            speed = 0f, damage = 20,
            slowMultiplier = 0.5f, slowDurationMs = 5_000L,
            slowSource = "snowpea"
        )
        world.update(16L)

        // Boss 不应被减速（slow 列表为空，currentSlowMultiplier=1.0）
        val effects = boss.get<StatusEffects>()!!
        assertTrue(
            "boss should not have slow effects, but has ${effects.slows.size}",
            effects.slows.isEmpty()
        )
        assertEquals(1f, effects.currentSlowMultiplier(), 0.0001f)
    }

    @Test
    fun melonSplashDoesNotSlowBoss() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileSystem())
        world.addSystem(ProjectileCollisionSystem(ctx))

        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        boss.get<Transform>()!!.x = 800f
        ProjectileFactory.createMelon(
            world = world, row = 2,
            startX = 800f, startY = boss.get<Transform>()!!.y,
            speed = 0f, damage = 60,
            slowMultiplier = 0.5f, slowDurationMs = 2_000L,
            splashRadius = 120f, splashDamageRatio = 0.3f
        )
        world.update(16L)

        // Boss 可以吃伤害，但不会被减速
        val effects = boss.get<StatusEffects>()!!
        assertTrue(effects.slows.isEmpty())
    }

    @Test
    fun statusEffectSystemKeepsBossColor_evenIfSlowInjected() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(StatusEffectSystem(ctx))

        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        // 人为注入 slow（绕开 ProjectileCollisionSystem 的免疫逻辑）
        boss.get<StatusEffects>()!!.addOrRefreshSlow("test", 0.5f, 2_000L)
        world.update(16L)

        // Boss 渲染色不应被改为 ZOMBIE_SLOWED；应保持阶段色 ZOMBIE_BOSS
        assertEquals(
            Renderable.ZOMBIE_BOSS,
            boss.get<Renderable>()!!.color
        )
    }

    @Test
    fun statusEffectSystemKeepsBossColor_evenIfFrozenInjected() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(StatusEffectSystem(ctx))

        val boss = ZombieFactory.create(world, ZombieConfig.BOSS_DRZOMBOSS, row = 2)
        world.update(0L)

        boss.get<StatusEffects>()!!.addOrRefreshFreeze(2_000L)
        world.update(16L)

        // 即便携带 freeze，Boss 颜色不切到 ZOMBIE_FROZEN
        assertEquals(
            Renderable.ZOMBIE_BOSS,
            boss.get<Renderable>()!!.color
        )
    }

    @Test
    fun basicZombieIsStillSlowed_regressionCheck() {
        // 防御性回归：普通僵尸应当仍能被减速，确认免疫逻辑只针对 Boss
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileCollisionSystem(ctx))

        val basic = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)
        val bx = basic.get<Transform>()!!.x

        ProjectileFactory.createIcePea(
            world = world, row = 2,
            startX = bx, startY = 0f,
            speed = 0f, damage = 20,
            slowMultiplier = 0.5f, slowDurationMs = 2_000L,
            slowSource = "snowpea"
        )
        world.update(16L)

        val effects = basic.get<StatusEffects>()!!
        assertEquals(1, effects.slows.size)
        assertEquals(0.5f, effects.currentSlowMultiplier(), 0.0001f)
    }
}
