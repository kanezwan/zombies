package com.zombies.game.systems

import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.PlantFactory
import com.zombies.game.input.InputEvent
import com.zombies.game.input.InputQueue
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShovelTest {

    private fun newCtx(): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun shovelRemovesPlantAndClearsOccupancy() {
        val ctx = newCtx()
        val queue = InputQueue()
        val world = World()
        world.addSystem(PlantingSystem(ctx, queue))
        world.addSystem(HealthSystem(ctx))

        // 先种一颗向日葵
        val plant = PlantFactory.create(world, PlantConfig.SUNFLOWER, row = 2, col = 3)
        ctx.occupancy.set(2, 3, plant.id)
        world.update(0L)
        assertTrue(ctx.occupancy.get(2, 3) == plant.id)

        // 打开铲子 + 点击该格
        ctx.shovelMode = true
        queue.offer(InputEvent.Down(virtualX = 500f, virtualY = 500f, row = 2, col = 3))
        world.update(16L) // PlantingSystem 处理 + HealthSystem 清理
        // 再跑一帧让 pendingRemove 生效
        world.update(16L)

        assertNull("植物应被移除", world.getEntity(plant.id))
        assertEquals(-1L, ctx.occupancy.get(2, 3))
        assertFalse("用完退出铲子模式", ctx.shovelMode)
    }

    @Test
    fun shovelDoesNothingOnEmptyCell() {
        val ctx = newCtx()
        val queue = InputQueue()
        val world = World()
        world.addSystem(PlantingSystem(ctx, queue))

        ctx.shovelMode = true
        queue.offer(InputEvent.Down(500f, 500f, row = 1, col = 1))
        world.update(16L)
        // 空格子点击后铲子仍被取消（我们的实现方式：点击空格也复位；实际游戏里允许取消）
        assertFalse(ctx.shovelMode)
    }

    @Test
    fun cardClickTogglesShovelOff() {
        val ctx = newCtx()
        val raw = InputQueue()
        val fwd = InputQueue()
        val world = World()
        world.addSystem(CardTouchSystem(ctx, raw, fwd))

        ctx.shovelMode = true
        // 模拟点击第一张卡牌位置（使用 cardRect）
        val r = CardHudRenderSystem.cardRect(0)
        raw.offer(InputEvent.Down(r.centerX(), r.centerY(), row = -1, col = -1))
        world.update(16L)

        assertFalse("选中卡牌后铲子应关闭", ctx.shovelMode)
        assertNotNull(ctx.selectedPlantType)
    }
}
