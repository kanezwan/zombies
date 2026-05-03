package com.zombies.game.systems

import com.zombies.game.components.Projectile
import com.zombies.game.components.Transform
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.core.Grid
import com.zombies.game.ecs.World
import com.zombies.game.entity.PlantFactory
import com.zombies.game.entity.ProjectileFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TorchwoodSystemTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun bulletPassingThroughTorchwoodDoublesDamage() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileSystem(ctx))

        // 在第 2 行第 4 列放一棵火炬
        val torch = PlantFactory.create(world, PlantConfig.TORCHWOOD, row = 2, col = 4)
        world.update(0L)
        ctx.occupancy.set(2, 4, torch.id)

        val torchCx = Grid.cellCenterX(4)
        // 子弹起点在火炬左侧稍远，向右飞；一帧推进使其中心正好落在火炬格
        val bullet = ProjectileFactory.createPea(
            world, row = 2, startX = torchCx - 100f, startY = 700f, speed = 100f / 0.016f, damage = 20
        )
        // 第一帧：推进 16ms → 位移 = speed*0.016 = 100；正好落在 torchCx
        world.update(16L)

        val proj = bullet.get<Projectile>()!!
        assertTrue(proj.fireEnhanced)
        assertEquals(40, proj.damage)
        assertEquals(4, proj.lastTorchedCol)
    }

    @Test
    fun bulletPassingSameTorchwoodTwiceOnlyBoostsOnce() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileSystem(ctx))

        val torch = PlantFactory.create(world, PlantConfig.TORCHWOOD, row = 2, col = 4)
        world.update(0L)
        ctx.occupancy.set(2, 4, torch.id)

        val torchCx = Grid.cellCenterX(4)
        val bullet = ProjectileFactory.createPea(
            world, row = 2, startX = torchCx - 10f, startY = 700f, speed = 100f, damage = 20
        )
        // 多帧内子弹停留在火炬格范围内
        world.update(16L)
        world.update(16L)
        world.update(16L)

        val proj = bullet.get<Projectile>()!!
        assertEquals(40, proj.damage) // 只加成一次
    }

    @Test
    fun bulletPassingTwoSeparateTorchwoodsDoublesTwice() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileSystem(ctx))

        val t1 = PlantFactory.create(world, PlantConfig.TORCHWOOD, row = 2, col = 3)
        val t2 = PlantFactory.create(world, PlantConfig.TORCHWOOD, row = 2, col = 6)
        world.update(0L)
        ctx.occupancy.set(2, 3, t1.id)
        ctx.occupancy.set(2, 6, t2.id)

        // 起点在火炬 1 中心附近，速度设为 0 —— 手动测试两次位置
        val bullet = ProjectileFactory.createPea(
            world, row = 2, startX = Grid.cellCenterX(3), startY = 700f, speed = 0f, damage = 10
        )
        world.update(16L)
        val proj = bullet.get<Projectile>()!!
        assertEquals(20, proj.damage) // 第一次加成

        // 手动把子弹移到火炬 2 位置
        bullet.get<Transform>()!!.x = Grid.cellCenterX(6)
        world.update(16L)
        assertEquals(40, proj.damage) // 第二次加成
    }

    @Test
    fun bulletWithoutTorchwoodKeepsOriginalDamage() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(ProjectileSystem(ctx))

        val bullet = ProjectileFactory.createPea(
            world, row = 2, startX = 500f, startY = 700f, speed = 500f, damage = 20
        )
        world.update(16L)

        val proj = bullet.get<Projectile>()!!
        assertEquals(20, proj.damage)
        assertFalse(proj.fireEnhanced)
    }
}
