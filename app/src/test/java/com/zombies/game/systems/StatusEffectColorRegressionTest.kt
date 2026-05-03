package com.zombies.game.systems

import com.zombies.game.components.Armor
import com.zombies.game.components.Renderable
import com.zombies.game.components.StatusEffects
import com.zombies.game.components.ZombieTag
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 回归测试：M9 新增的 newspaper/football/jester 在 slow debuff 到期后必须还原为各自本色，
 * 而不是误还原为 ZOMBIE_BASIC。
 */
class StatusEffectColorRegressionTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun newspaperRestoresToNewspaperColor_notBasic() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(StatusEffectSystem(ctx))
        val z = ZombieFactory.create(world, ZombieConfig.NEWSPAPER, row = 2)
        world.update(0L)

        // 施加 slow，再到期
        z.get<StatusEffects>()!!.addOrRefreshSlow("snowpea", 0.5f, 100L)
        world.update(50L)
        assertEquals(Renderable.ZOMBIE_SLOWED, z.get<Renderable>()!!.color)
        world.update(200L) // 到期
        assertEquals(Renderable.ZOMBIE_NEWSPAPER, z.get<Renderable>()!!.color)
    }

    @Test
    fun newspaperBrokenArmorRestoresToEnragedColor() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(StatusEffectSystem(ctx))
        val z = ZombieFactory.create(world, ZombieConfig.NEWSPAPER, row = 2)
        world.update(0L)

        // 模拟护甲破碎（不经过 collision 系统，直接把 armor.hp 清零）
        z.get<Armor>()!!.absorb(9999)
        // 施加 slow 再到期
        z.get<StatusEffects>()!!.addOrRefreshSlow("snowpea", 0.5f, 100L)
        world.update(50L)
        world.update(200L)
        // 到期后应当还原为暴怒红，而不是初始灰蓝
        assertEquals(Renderable.ZOMBIE_NEWSPAPER_ENRAGED, z.get<Renderable>()!!.color)
    }

    @Test
    fun footballRestoresToFootballColor() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(StatusEffectSystem(ctx))
        val z = ZombieFactory.create(world, ZombieConfig.FOOTBALL, row = 2)
        world.update(0L)
        z.get<StatusEffects>()!!.addOrRefreshSlow("snowpea", 0.5f, 100L)
        world.update(50L)
        world.update(200L)
        assertEquals(Renderable.ZOMBIE_FOOTBALL, z.get<Renderable>()!!.color)
    }

    @Test
    fun jesterRestoresToJesterColor() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(StatusEffectSystem(ctx))
        val z = ZombieFactory.create(world, ZombieConfig.JESTER, row = 2)
        world.update(0L)
        z.get<StatusEffects>()!!.addOrRefreshSlow("snowpea", 0.5f, 100L)
        world.update(50L)
        world.update(200L)
        assertEquals(Renderable.ZOMBIE_JESTER, z.get<Renderable>()!!.color)
    }

    @Test
    fun sanityCheck_zombieTagTypesAreDistinct() {
        // 确保测试依赖的 ZombieTag.type 约定没改
        assertEquals("newspaper", ZombieConfig.NEWSPAPER.type)
        assertEquals("football", ZombieConfig.FOOTBALL.type)
        assertEquals("jester", ZombieConfig.JESTER.type)
    }
}
