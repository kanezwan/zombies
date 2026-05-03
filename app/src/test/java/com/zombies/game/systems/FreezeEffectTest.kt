package com.zombies.game.systems

import com.zombies.game.components.Renderable
import com.zombies.game.components.StatusEffects
import com.zombies.game.components.Transform
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 冻结（freeze）机制测试：multiplier=0 应令僵尸完全停住，并渲染为 ZOMBIE_FROZEN；
 * 到期后恢复移动 + 还原本色。
 */
class FreezeEffectTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun freezeStopsZombie_andAppliesFrozenColor() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(StatusEffectSystem(ctx))
        world.addSystem(ZombieMoveSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)

        z.get<StatusEffects>()!!.addOrRefreshFreeze(1000L)
        val beforeX = z.get<Transform>()!!.x
        world.update(500L)
        val afterX = z.get<Transform>()!!.x
        // 冻结期间完全不动
        assertEquals(beforeX, afterX, 0.001f)
        // 渲染切成 FROZEN
        assertEquals(Renderable.ZOMBIE_FROZEN, z.get<Renderable>()!!.color)
    }

    @Test
    fun freezeExpires_thenZombieWalksAgain_andColorRestores() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(StatusEffectSystem(ctx))
        world.addSystem(ZombieMoveSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)

        z.get<StatusEffects>()!!.addOrRefreshFreeze(200L)
        world.update(250L) // 过期
        val startX = z.get<Transform>()!!.x
        world.update(1000L)
        val endX = z.get<Transform>()!!.x
        // 冻结到期后，僵尸继续以 baseSpeed=22 向左走
        assertTrue("should have moved after freeze expired", startX - endX > 15f)
        // 本色还原为 BASIC
        assertEquals(Renderable.ZOMBIE_BASIC, z.get<Renderable>()!!.color)
    }

    @Test
    fun freezeRefreshesInstead_ofStacking() {
        val effects = StatusEffects()
        effects.addOrRefreshFreeze(500L)
        effects.addOrRefreshFreeze(1000L) // 刷新
        assertEquals(1, effects.slows.size)
        // tick 到第一次时长应该还没到期（因为被刷新到了 1000ms）
        effects.tick(700L)
        assertFalse(effects.slows.isEmpty())
        assertEquals(0f, effects.currentSlowMultiplier(), 0.0001f)
    }

    @Test
    fun removeBySourceClearsFreeze() {
        val effects = StatusEffects()
        effects.addOrRefreshFreeze(1000L)
        assertTrue(effects.removeBySource("freeze"))
        assertEquals(1f, effects.currentSlowMultiplier(), 0.0001f)
        // 再次移除应失败
        assertFalse(effects.removeBySource("freeze"))
    }
}
