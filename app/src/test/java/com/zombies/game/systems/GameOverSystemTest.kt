package com.zombies.game.systems

import com.zombies.game.components.Transform
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.WaveConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import com.zombies.game.save.ClearStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class GameOverSystemTest {

    private fun newCtx(wave: WaveConfig = WaveConfig.LEVEL_1): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type },
        wave = wave
    )

    @Test
    fun defeatFiresWhenZombieCrossesHouseLine() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(GameOverSystem(ctx))

        var defeatFired = 0
        ctx.onDefeat = { defeatFired++ }

        val z = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)
        // 直接把僵尸拖到房屋线左侧
        z.get<Transform>()!!.x = GameOverSystem.HOUSE_LINE - 10f
        world.update(16L)

        assertSame(GameContext.State.DEFEAT, ctx.state)
        assertEquals(1, defeatFired)

        // 再次 tick：系统已停工，回调不应重复
        world.update(16L)
        assertEquals(1, defeatFired)
    }

    @Test
    fun victoryFiresWhenAllSpawnedAndFieldClear() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(GameOverSystem(ctx))

        var clearStats: ClearStats? = null
        ctx.onVictory = { stats -> clearStats = stats }

        // 模拟：所有波次已生成 + 时间已过 + 场上无僵尸
        ctx.nextSpawnIndex = ctx.wave.entries.size
        ctx.levelElapsedMs = ctx.wave.totalDurationMs + 1_000L
        // 阳光 42 用于验证 ClearStats.sunLeft
        ctx.economy.addSun(42 - ctx.economy.sun)

        world.update(16L)

        assertSame(GameContext.State.VICTORY, ctx.state)
        assertNotNull(clearStats)
        assertEquals(ctx.wave.totalDurationMs + 1_000L, clearStats!!.timeMs)
        assertEquals(ctx.economy.sun, clearStats!!.sunLeft)
    }

    @Test
    fun victoryDoesNotFireWhenZombiesRemain() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(GameOverSystem(ctx))

        var fired = 0
        ctx.onVictory = { fired++ }

        // 场上还剩一只僵尸
        ZombieFactory.create(world, ZombieConfig.BASIC, row = 1)
        ctx.nextSpawnIndex = ctx.wave.entries.size
        ctx.levelElapsedMs = ctx.wave.totalDurationMs + 1_000L

        world.update(16L)

        assertSame(GameContext.State.RUNNING, ctx.state)
        assertEquals(0, fired)
    }

    @Test
    fun callbackNotFiredIfNotSet() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(GameOverSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)
        z.get<Transform>()!!.x = GameOverSystem.HOUSE_LINE - 10f
        world.update(16L)

        // 仅检查状态切换正常，onDefeat 为 null 时不崩溃
        assertSame(GameContext.State.DEFEAT, ctx.state)
        assertNull(ctx.onDefeat) // 未被赋值
    }
}
