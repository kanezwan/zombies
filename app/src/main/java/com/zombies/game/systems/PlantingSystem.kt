package com.zombies.game.systems

import com.zombies.game.audio.SoundId
import com.zombies.game.components.FloatingText
import com.zombies.game.components.Health
import com.zombies.game.components.Pickable
import com.zombies.game.components.Sun
import com.zombies.game.components.Transform
import com.zombies.game.core.Grid
import com.zombies.game.ecs.Entity
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.entity.PlantFactory
import com.zombies.game.input.InputEvent
import com.zombies.game.input.InputQueue
import com.zombies.game.level.GameContext

/**
 * 玩家交互系统：处理 Down 事件。
 * 优先级高（先执行），确保拾取/种植发生在各逻辑系统之前。
 *
 * 规则：
 *  1. 命中一颗阳光 → 拾取（加阳光，移除实体）
 *  2. 铲子模式 + 命中已种植的格子 → 拔除植物（不退还阳光，保持原作手感）
 *  3. 否则若已选中植物 + 格子空 + 阳光够 + 冷却就绪 → 种植
 *  4. 其他情况忽略
 */
class PlantingSystem(
    private val ctx: GameContext,
    private val inputQueue: InputQueue
) : GameSystem(priority = 0) {

    override fun update(world: World, dtMs: Long) {
        val events = inputQueue.drain()
        for (ev in events) {
            if (ev is InputEvent.Down) handleDown(world, ev)
        }
    }

    private fun handleDown(world: World, ev: InputEvent.Down) {
        // 1) 优先拾取阳光
        val picked = findSunAt(world, ev.virtualX, ev.virtualY)
        if (picked != null) {
            val sun = picked.require<Sun>()
            val pickedT = picked.get<Transform>()
            ctx.economy.addSun(sun.value)
            world.removeEntity(picked.id)
            ctx.audio.play(SoundId.SUN_PICK)
            // 在拾取位置生成 "+50" 飘字反馈
            if (pickedT != null) {
                val ft = world.createEntity()
                ft.add(Transform(pickedT.x, pickedT.y))
                ft.add(FloatingText(text = "+${sun.value}"))
            }
            return
        }

        // 2) 非草坪点击忽略
        if (!Grid.isValidCell(ev.row, ev.col)) return

        // 3) 铲子模式：移除已种植物
        if (ctx.shovelMode) {
            val plantId = ctx.occupancy.get(ev.row, ev.col)
            if (plantId > 0L) {
                val plant = world.getEntity(plantId)
                // 通过扣满 HP 让 HealthSystem 统一清理 + 释放格子
                plant?.get<Health>()?.let { it.damage(it.hp) }
                    ?: run {
                        // 无 Health（理论上不会发生），手动移除
                        ctx.occupancy.clear(ev.row, ev.col)
                        world.removeEntity(plantId)
                    }
                ctx.audio.play(SoundId.SHOVEL)
            }
            ctx.shovelMode = false // 用完退出铲子模式
            return
        }

        // 4) 尝试种植
        val type = ctx.selectedPlantType ?: return
        val cfg = ctx.plants[type] ?: return
        if (!ctx.occupancy.isEmpty(ev.row, ev.col)) return
        if (!ctx.economy.isReady(type)) return
        if (!ctx.economy.trySpend(cfg.cost)) return

        val entity = PlantFactory.create(world, cfg, ev.row, ev.col)
        ctx.occupancy.set(ev.row, ev.col, entity.id)
        ctx.economy.triggerCooldown(type, cfg.cooldownMs)
        ctx.selectedPlantType = null
        ctx.plantedThisRun++
        ctx.audio.play(SoundId.PLANT)
    }

    /** 返回命中的阳光实体（最多一个），未命中返回 null */
    private fun findSunAt(world: World, x: Float, y: Float): Entity? {
        var hit: Entity? = null
        world.forEachWith2<Sun, Pickable> { e, _, pick ->
            if (hit != null) return@forEachWith2
            val t = e.get<Transform>() ?: return@forEachWith2
            val dx = x - t.x
            val dy = y - t.y
            if (dx * dx + dy * dy <= pick.radius * pick.radius) {
                hit = e
            }
        }
        return hit
    }
}
