package com.zombies.game.systems

import com.zombies.game.components.Armor
import com.zombies.game.components.Health
import com.zombies.game.components.Transform
import com.zombies.game.components.ZombieMover
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.ProjectileFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Newspaper 报纸护甲 + 暴走测试。
 */
class NewspaperEnrageTest {

    private fun newCtx() = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun armorAbsorbsBeforeBody() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileCollisionSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.NEWSPAPER, row = 0)
        world.update(0L)
        val armor = z.get<Armor>()!!
        val hp = z.get<Health>()!!
        val initArmor = armor.hp
        val initBody = hp.hp
        val zx = z.get<Transform>()!!.x

        // 一发 100 伤害，全部吸入护甲
        ProjectileFactory.createPea(world, row = 0, startX = zx, startY = 0f, speed = 0f, damage = 100)
        world.update(0L)
        world.update(16L)

        assertEquals(initArmor - 100, armor.hp)
        assertEquals(initBody, hp.hp)
        assertFalse(armor.broken)
    }

    @Test
    fun armorBreaksAndBodyTakesOverflow() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileCollisionSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.NEWSPAPER, row = 0)
        world.update(0L)
        val armor = z.get<Armor>()!!
        val hp = z.get<Health>()!!
        val initBody = hp.hp
        val zx = z.get<Transform>()!!.x
        val initArmor = armor.hp

        // 超额伤害：全部吃护甲后剩余打本体
        val shot = initArmor + 50
        ProjectileFactory.createPea(world, row = 0, startX = zx, startY = 0f, speed = 0f, damage = shot)
        world.update(0L)
        world.update(16L)

        assertTrue(armor.broken)
        assertEquals(0, armor.hp)
        assertEquals(initBody - 50, hp.hp)
    }

    @Test
    fun armorBreakEnragesNewspaperSpeed() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileCollisionSystem(ctx))

        val z = ZombieFactory.create(world, ZombieConfig.NEWSPAPER, row = 0)
        world.update(0L)
        val baseSpeed = z.get<ZombieMover>()!!.baseSpeed
        val zx = z.get<Transform>()!!.x
        val initArmor = z.get<Armor>()!!.hp

        // 一发打爆护甲
        ProjectileFactory.createPea(world, row = 0, startX = zx, startY = 0f, speed = 0f, damage = initArmor + 10)
        world.update(0L)
        world.update(16L)

        val afterSpeed = z.get<ZombieMover>()!!.baseSpeed
        // 默认 NEWSPAPER.enragedSpeedMultiplier = 2.5f
        assertEquals(baseSpeed * 2.5f, afterSpeed, 0.01f)
    }
}
