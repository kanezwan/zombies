package com.zombies.game.systems

import com.zombies.game.components.Health
import com.zombies.game.components.Transform
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.core.Grid
import com.zombies.game.ecs.World
import com.zombies.game.entity.PlantFactory
import com.zombies.game.entity.ProjectileFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M11: 验证 [BossCarSystem] 行为。
 *
 *  - 扔出的车对同行植物造成 400 伤（每列只命中一次，幂等）
 *  - 非同行植物不受影响
 *  - 车飞出左侧边界（x <= LEFT_BOUND）后被销毁
 *  - 车不伤害僵尸
 */
class BossCarSystemTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun carDamagesAllPlantsInSameRow() {
        val world = World()
        world.addSystem(BossCarSystem())

        // 同一行放 3 株豌豆射手
        val p1 = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 1)
        val p2 = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 3)
        val p3 = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 5)
        // 异行对照
        val pOff = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 0, col = 3)
        world.update(0L)

        // 把车手动移到一个"已经碾过三列"的中间位置：从很左侧贴到 col=1，逐帧移动碾轧
        val carStartX = Grid.ORIGIN_X + Grid.WIDTH + 100f
        val car = ProjectileFactory.createCar(
            world = world,
            row = 2,
            startX = carStartX,
            startY = Grid.cellCenterY(2),
            speed = 0f,  // 手动控制车位置
            damage = 400
        )
        world.update(0L)

        val hpMax = PlantConfig.PEASHOOTER.hp

        // 第一帧：把车挪到 col=5
        car.get<Transform>()!!.x = Grid.cellCenterX(5)
        world.update(16L)
        assertEquals(hpMax - 400, p3.get<Health>()!!.hp)
        assertEquals(hpMax, p2.get<Health>()!!.hp)

        // 第二帧：把车挪到 col=3
        car.get<Transform>()!!.x = Grid.cellCenterX(3)
        world.update(16L)
        assertEquals(hpMax - 400, p2.get<Health>()!!.hp)

        // 第三帧：挪到 col=1
        car.get<Transform>()!!.x = Grid.cellCenterX(1)
        world.update(16L)
        assertEquals(hpMax - 400, p1.get<Health>()!!.hp)

        // 异行从头到尾不变
        assertEquals(hpMax, pOff.get<Health>()!!.hp)
    }

    @Test
    fun carDoesNotDamageSameColumnTwice_idempotent() {
        val world = World()
        world.addSystem(BossCarSystem())

        val plant = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 4)
        world.update(0L)

        val car = ProjectileFactory.createCar(
            world = world, row = 2,
            startX = Grid.cellCenterX(4),
            startY = Grid.cellCenterY(2),
            speed = 0f, damage = 400
        )
        world.update(0L)
        assertNotNull(world.getEntity(car.id))

        val hpMax = PlantConfig.PEASHOOTER.hp

        // 第一帧：命中 col=4
        world.update(16L)
        val hpAfter1 = plant.get<Health>()!!.hp
        assertEquals(hpMax - 400, hpAfter1)

        // 第二帧：车仍在同列 → 不应再次扣血
        world.update(16L)
        assertEquals(hpAfter1, plant.get<Health>()!!.hp)
    }

    @Test
    fun carDoesNotDamageZombies() {
        val world = World()
        world.addSystem(BossCarSystem())

        val zombie = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)

        val zx = zombie.get<Transform>()!!.x
        ProjectileFactory.createCar(
            world = world, row = 2,
            startX = zx,
            startY = Grid.cellCenterY(2),
            speed = 0f, damage = 400
        )
        world.update(0L)

        world.update(16L)
        assertEquals(ZombieConfig.BASIC.hp, zombie.get<Health>()!!.hp)
    }

    @Test
    fun carIsRemovedWhenFlyingPastLeftBound() {
        val world = World()
        world.addSystem(BossCarSystem())

        val car = ProjectileFactory.createCar(
            world = world, row = 2,
            startX = Grid.cellCenterX(4),
            startY = Grid.cellCenterY(2),
            speed = 0f, damage = 400
        )
        world.update(0L)
        assertNotNull(world.getEntity(car.id))

        // 手动挪到极左
        car.get<Transform>()!!.x = BossCarSystem.LEFT_BOUND - 10f
        world.update(16L)
        assertNull("car should be removed past left bound", world.getEntity(car.id))
    }

    @Test
    fun carOnlyHitsWithinHalfWidthRange() {
        val world = World()
        world.addSystem(BossCarSystem())

        val plant = PlantFactory.create(world, PlantConfig.PEASHOOTER, row = 2, col = 4)
        world.update(0L)

        val car = ProjectileFactory.createCar(
            world = world, row = 2,
            // 位于 col=4 中心的两倍车半宽之外 —— 不应命中
            startX = Grid.cellCenterX(4) + BossCarSystem.CAR_HALF_W + 20f,
            startY = Grid.cellCenterY(2),
            speed = 0f, damage = 400
        )
        world.update(0L)

        world.update(16L)
        // 此时车中心距植物中心 > CAR_HALF_W，不应扣血
        assertEquals(PlantConfig.PEASHOOTER.hp, plant.get<Health>()!!.hp)

        // 把车挪到植物中心 → 命中
        car.get<Transform>()!!.x = Grid.cellCenterX(4)
        world.update(16L)
        assertTrue(plant.get<Health>()!!.hp < PlantConfig.PEASHOOTER.hp)
    }
}
