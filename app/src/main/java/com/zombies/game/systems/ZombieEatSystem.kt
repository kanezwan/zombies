package com.zombies.game.systems

import com.zombies.game.components.GridCell
import com.zombies.game.components.Health
import com.zombies.game.components.PlantTag
import com.zombies.game.components.Renderable
import com.zombies.game.components.Transform
import com.zombies.game.components.WalkBob
import com.zombies.game.components.ZombieEater
import com.zombies.game.components.ZombieMover
import com.zombies.game.core.Grid
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext

/**
 * 僵尸啃食系统：
 *  - 僵尸所在列（根据 Transform.x 计算）若有同行植物 → 进入 EATING，定时扣血
 *  - 无植物则恢复 WALKING
 *
 * 变色策略：吃植物时统一变红（ZOMBIE_EATING）；恢复行走时由 StatusEffectSystem 接管颜色
 * （本系统不再主动写基础色，避免 1 帧内颜色被多个系统覆盖的闪烁）。
 *
 * priority = 46
 */
class ZombieEatSystem(private val ctx: GameContext) : GameSystem(priority = 46) {

    override fun update(world: World, dtMs: Long) {
        if (ctx.state != GameContext.State.RUNNING) return
        world.forEachWith2<ZombieEater, ZombieMover> { entity, eater, mover ->
            val t = entity.get<Transform>() ?: return@forEachWith2
            val cell = entity.get<GridCell>() ?: return@forEachWith2
            val row = cell.row

            val col = xToCol(t.x)
            val plantEntityId = if (col in 0 until Grid.COLS) ctx.occupancy.get(row, col) else -1L

            if (plantEntityId > 0L) {
                mover.state = ZombieMover.State.EATING
                // 啃食期间切换视觉模式：停止上下弹跳，启用"前后探头"模仿啃咬动作
                entity.get<WalkBob>()?.let { bob ->
                    bob.active = false
                    // leanPx = 4 → 前后 ±4px；频率保持，同相位 → 有节奏的啃
                    bob.eatLeanPx = 4f
                }
                if (eater.targetEntityId != plantEntityId) {
                    eater.targetEntityId = plantEntityId
                    eater.elapsedMs = 0L
                }
                eater.elapsedMs += dtMs
                if (eater.elapsedMs >= eater.intervalMs) {
                    eater.elapsedMs -= eater.intervalMs
                    val plant = world.getEntity(plantEntityId)
                    val hp = plant?.get<Health>()
                    if (plant == null || hp == null || !plant.has<PlantTag>()) {
                        eater.targetEntityId = -1L
                        mover.state = ZombieMover.State.WALKING
                    } else {
                        hp.damage(eater.damage)
                    }
                }
                val r = entity.get<Renderable>()
                if (r != null && r.color != Renderable.ZOMBIE_EATING) {
                    replaceColor(entity, Renderable.ZOMBIE_EATING)
                }
            } else {
                if (mover.state != ZombieMover.State.WALKING) {
                    mover.state = ZombieMover.State.WALKING
                    eater.targetEntityId = -1L
                    eater.elapsedMs = 0L
                    // 颜色还原交给 StatusEffectSystem 下一帧处理
                }
                // 恢复走路抖动，关闭啃咬前冲
                entity.get<WalkBob>()?.let { bob ->
                    bob.active = true
                    bob.eatLeanPx = 0f
                }
            }
        }
    }

    /** 僵尸 x 坐标 → 所在列；草坪外返回 -1。 */
    private fun xToCol(x: Float): Int {
        if (x < Grid.ORIGIN_X || x >= Grid.ORIGIN_X + Grid.WIDTH) return -1
        return ((x - Grid.ORIGIN_X) / Grid.CELL_W).toInt().coerceIn(0, Grid.COLS - 1)
    }

    /** Renderable 是 data class 且 color 为 val —— 此处用替换组件实例的方式改色。 */
    private fun replaceColor(entity: com.zombies.game.ecs.Entity, color: Int) {
        val old = entity.get<Renderable>() ?: return
        entity.add(
            Renderable(
                shape = old.shape,
                color = color,
                width = old.width,
                height = old.height,
                zOrder = old.zOrder
            )
        )
    }
}
