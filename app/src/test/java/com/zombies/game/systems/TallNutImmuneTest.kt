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
import org.junit.Test

class TallNutImmuneTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun poleVaultDoesNotJumpOverTallNut() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(PoleVaultSystem(ctx))

        // 唯一的前方植物是高坚果
        val tallnut = PlantFactory.create(world, PlantConfig.TALLNUT, row = 2, col = 5)
        world.update(0L)
        ctx.occupancy.set(2, 5, tallnut.id)

        val z = ZombieFactory.create(world, ZombieConfig.POLEVAULT, row = 2)
        world.update(0L)
        val cx = Grid.cellCenterX(5)
        val zt = z.get<Transform>()!!
        zt.x = cx + 100f // 触发距离内
        val beforeX = zt.x

        world.update(16L)

        val pole = z.get<PoleVault>()!!
        assertFalse("TallNut should be immune to pole vault", pole.used)
        assertEquals("Zombie position should not be teleported", beforeX, zt.x, 0.01f)
    }

    @Test
    fun poleVaultSkipsPumpkinWhenOnlyImmunePlantsInFront() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(PoleVaultSystem(ctx))

        // 僵尸正前方只有南瓜头（免疫）；由于南瓜头被视为不可跳越目标，
        // 僵尸不会发起撑杆跳，会继续走近去啃它（此测试只验证不跳）
        val pumpkin = PlantFactory.create(world, PlantConfig.PUMPKIN, row = 2, col = 5)
        world.update(0L)
        ctx.occupancy.set(2, 5, pumpkin.id)

        val z = ZombieFactory.create(world, ZombieConfig.POLEVAULT, row = 2)
        world.update(0L)
        val cx = Grid.cellCenterX(5)
        z.get<Transform>()!!.x = cx + 120f // 本应触发跳跃的距离

        world.update(16L)

        val pole = z.get<PoleVault>()!!
        assertFalse("Pumpkin is immune; pole vault should not trigger", pole.used)
    }
}
