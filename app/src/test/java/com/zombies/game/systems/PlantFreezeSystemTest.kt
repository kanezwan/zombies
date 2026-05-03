package com.zombies.game.systems

import com.zombies.game.components.PlantFreeze
import com.zombies.game.components.Renderable
import com.zombies.game.components.Shooter
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.PlantFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M11: 验证 [PlantFreezeSystem] + ShooterSystem/ProducerSystem 对 PlantFreeze 的配合。
 *
 *  - PlantFreeze 挂上后：Renderable 被覆盖为冰蓝色 PLANT_FROZEN
 *  - 冻结期间：ShooterSystem 跳过射击（elapsedMs 清零）
 *  - 到期：PlantFreeze 自动移除；Renderable 还原本色
 */
class PlantFreezeSystemTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun freezeTintsPlantToFrozenColor() {
        val world = World()
        world.addSystem(PlantFreezeSystem())

        val plant = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 3)
        world.update(0L)

        plant.add(PlantFreeze(remainingMs = 1_000L))
        world.update(16L)

        assertEquals(
            Renderable.PLANT_FROZEN,
            plant.get<Renderable>()!!.color
        )
    }

    @Test
    fun freezeExpires_andRestoresOriginalColor() {
        val world = World()
        world.addSystem(PlantFreezeSystem())

        val plant = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 3)
        world.update(0L)

        plant.add(PlantFreeze(remainingMs = 200L))
        world.update(16L)
        assertEquals(Renderable.PLANT_FROZEN, plant.get<Renderable>()!!.color)

        // 把整个 200ms 推进过去
        world.update(300L)
        assertNull("PlantFreeze should be removed", plant.get<PlantFreeze>())
        assertEquals(
            "color should restore to peashooter green",
            Renderable.PLANT_PEASHOOTER,
            plant.get<Renderable>()!!.color
        )
    }

    @Test
    fun shooterIsBlocked_whilePlantIsFrozen() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(PlantFreezeSystem())
        world.addSystem(ShooterSystem(shouldCheckZombie = true))

        val plant = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 1)
        // 同行有僵尸，否则 ShooterSystem 本来就不射
        ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)

        plant.add(PlantFreeze(remainingMs = 5_000L))

        // 推进 3 秒（足够多个攻击间隔）—— 不应产生任何子弹
        var totalShooterElapsed = 0L
        for (i in 0 until 150) {
            world.update(20L)
            totalShooterElapsed += 20L
        }
        // ShooterSystem 应当保持 elapsedMs=0（冻结分支直接清零）
        assertEquals(0L, plant.get<Shooter>()!!.elapsedMs)
    }

    @Test
    fun shooterResumes_afterFreezeExpires() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(PlantFreezeSystem())
        world.addSystem(ShooterSystem(shouldCheckZombie = true))

        val plant = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 1)
        ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)

        plant.add(PlantFreeze(remainingMs = 100L))

        // 推进到过期后
        world.update(150L)
        assertNull(plant.get<PlantFreeze>())

        // 再推进 2 倍攻击间隔：Shooter 应恢复射击
        val interval = PlantConfig.PEASHOOTER.attackIntervalMs
        world.update(interval * 2)
        assertTrue(
            "shooter should have fired at least once after freeze expired",
            plant.get<Shooter>()!!.elapsedMs < interval  // 刚射完会重置
        )
    }

    @Test
    fun freezeComponentIsRemovedAfterExpiry() {
        val world = World()
        world.addSystem(PlantFreezeSystem())

        val plant = PlantFactory.create(world, PlantConfig.SUNFLOWER, row = 2, col = 3)
        world.update(0L)

        plant.add(PlantFreeze(remainingMs = 50L))
        world.update(16L)
        assertNotNull(plant.get<PlantFreeze>())

        world.update(100L)
        assertNull(plant.get<PlantFreeze>())
    }

    @Test
    fun freezeOnMultiplePlants_allGetTinted() {
        val world = World()
        world.addSystem(PlantFreezeSystem())

        val p1 = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 0, col = 1)
        val p2 = PlantFactory.create(world, PlantConfig.SUNFLOWER, row = 1, col = 2)
        val p3 = PlantFactory.create(world, PlantConfig.WALLNUT, row = 2, col = 3)
        world.update(0L)

        p1.add(PlantFreeze(remainingMs = 500L))
        p2.add(PlantFreeze(remainingMs = 500L))
        p3.add(PlantFreeze(remainingMs = 500L))
        world.update(16L)

        assertEquals(Renderable.PLANT_FROZEN, p1.get<Renderable>()!!.color)
        assertEquals(Renderable.PLANT_FROZEN, p2.get<Renderable>()!!.color)
        assertEquals(Renderable.PLANT_FROZEN, p3.get<Renderable>()!!.color)

        // 到期后各自还原本色
        world.update(600L)
        assertEquals(Renderable.PLANT_PEASHOOTER, p1.get<Renderable>()!!.color)
        assertEquals(Renderable.PLANT_SUNFLOWER, p2.get<Renderable>()!!.color)
        assertEquals(Renderable.PLANT_WALLNUT, p3.get<Renderable>()!!.color)
        assertFalse(p1.has<PlantFreeze>())
    }
}
