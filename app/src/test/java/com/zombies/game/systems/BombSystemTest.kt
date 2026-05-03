package com.zombies.game.systems

import com.zombies.game.components.BombFuse
import com.zombies.game.components.Health
import com.zombies.game.components.Transform
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.PlantFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BombSystemTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun bombExplodesAfterFuseAndKillsZombiesInRadius() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(BombSystem(ctx))
        world.addSystem(HealthSystem(ctx))

        // 种樱桃炸弹
        val bomb = PlantFactory.create(world, PlantConfig.CHERRY_BOMB, row = 2, col = 4)
        world.update(0L)

        // 在樱桃同位置生成两只普通僵尸，再远处放一只（超出半径）
        val nearZ1 = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        val nearZ2 = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        val farZ = ZombieFactory.create(world, ZombieConfig.BASIC, row = 2)
        world.update(0L)
        // 手动摆位：把两只近僵尸拉到炸弹正上方，远僵尸拉到 1000 外
        val bombT = bomb.get<Transform>()!!
        nearZ1.get<Transform>()!!.apply { x = bombT.x + 50f; y = bombT.y }
        nearZ2.get<Transform>()!!.apply { x = bombT.x - 100f; y = bombT.y }
        farZ.get<Transform>()!!.apply { x = bombT.x + 1000f; y = bombT.y }

        // 引信 < 1200ms：不爆
        world.update(1000L)
        assertFalse(bomb.get<BombFuse>()!!.triggered)

        // 再推进 300ms → 累计 1300ms，应爆炸
        world.update(300L)
        assertTrue(bomb.get<BombFuse>()!!.triggered)

        // 近僵尸 HP 应当被 1800 伤害一击致死（basic=200）
        assertEquals(0, nearZ1.get<Health>()!!.hp)
        assertEquals(0, nearZ2.get<Health>()!!.hp)
        // 远僵尸血量无变化
        assertEquals(200, farZ.get<Health>()!!.hp)

        // HealthSystem 下一帧清理 死掉的实体（炸弹自身 + 两只近僵尸）
        world.update(16L)
        assertEquals(null, world.getEntity(bomb.id))
        assertEquals(null, world.getEntity(nearZ1.id))
        assertEquals(null, world.getEntity(nearZ2.id))
        assertNotNull(world.getEntity(farZ.id))
    }

    @Test
    fun bombTriggeredOnceEvenOverTime() {
        val ctx = newCtx()
        val world = World()
        world.addSystem(BombSystem(ctx))
        val bomb = PlantFactory.create(world, PlantConfig.CHERRY_BOMB, row = 0, col = 0)
        world.update(0L)
        // 推进很长时间，仍只会 triggered 一次
        world.update(5_000L)
        assertTrue(bomb.get<BombFuse>()!!.triggered)
        world.update(5_000L)
        assertTrue(bomb.get<BombFuse>()!!.triggered)
    }

    @Test
    fun bombDoesNotExplodeWhenGameNotRunning() {
        val ctx = newCtx().apply { state = GameContext.State.DEFEAT }
        val world = World()
        world.addSystem(BombSystem(ctx))
        val bomb = PlantFactory.create(world, PlantConfig.CHERRY_BOMB, row = 0, col = 0)
        world.update(0L)
        world.update(5_000L)
        assertFalse(bomb.get<BombFuse>()!!.triggered)
    }
}
