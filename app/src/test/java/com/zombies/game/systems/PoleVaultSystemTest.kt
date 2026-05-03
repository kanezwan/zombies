package com.zombies.game.systems

import com.zombies.game.components.PoleVault
import com.zombies.game.components.Transform
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.core.Grid
import com.zombies.game.ecs.World
import com.zombies.game.entity.PlantFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoleVaultSystemTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun poleVaultJumpsOverNearestPlantThenUsed() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(PoleVaultSystem(ctx))

        // 在第 2 行第 5 列种一棵豌豆射手
        val plant = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 5)
        world.update(0L)
        ctx.occupancy.set(2, 5, plant.id)

        // 撑杆跳僵尸放在比该植物中心略右、<=180 距离处
        val z = ZombieFactory.create(world, ZombieConfig.POLEVAULT, row = 2)
        world.update(0L)
        val plantCx = Grid.cellCenterX(5)
        z.get<Transform>()!!.x = plantCx + 100f // 触发距离内

        world.update(16L)

        val pole = z.get<PoleVault>()!!
        assertTrue(pole.used)
        // 落地在第 4 列中心
        assertEquals(Grid.cellCenterX(4), z.get<Transform>()!!.x, 0.5f)
    }

    @Test
    fun poleVaultDoesNotJumpWhenOutOfRange() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(PoleVaultSystem(ctx))

        val plant = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 5)
        world.update(0L)
        ctx.occupancy.set(2, 5, plant.id)

        val z = ZombieFactory.create(world, ZombieConfig.POLEVAULT, row = 2)
        world.update(0L)
        val plantCx = Grid.cellCenterX(5)
        z.get<Transform>()!!.x = plantCx + 400f // 超出 jumpDistance=180

        world.update(16L)

        val pole = z.get<PoleVault>()!!
        assertFalse(pole.used)
    }

    @Test
    fun poleVaultUsedOnlyOnce() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(PoleVaultSystem(ctx))

        val plant1 = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 5)
        val plant2 = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 3)
        world.update(0L)
        ctx.occupancy.set(2, 5, plant1.id)
        ctx.occupancy.set(2, 3, plant2.id)

        val z = ZombieFactory.create(world, ZombieConfig.POLEVAULT, row = 2)
        world.update(0L)
        val plantCx = Grid.cellCenterX(5)
        z.get<Transform>()!!.x = plantCx + 100f

        world.update(16L)
        val afterFirst = z.get<Transform>()!!.x
        assertTrue(z.get<PoleVault>()!!.used)

        // 把僵尸拉到第二棵植物附近 —— 不应再跳
        val plant2Cx = Grid.cellCenterX(3)
        z.get<Transform>()!!.x = plant2Cx + 100f
        world.update(16L)
        // 没有跳跃，位置应保持刚赋值的 100+plant2Cx（即没被瞬移到第 2 列中心）
        assertEquals(plant2Cx + 100f, z.get<Transform>()!!.x, 0.5f)
    }
}
