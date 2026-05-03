package com.zombies.game.systems

import com.zombies.game.components.Projectile
import com.zombies.game.components.Torchwood
import com.zombies.game.components.Transform
import com.zombies.game.components.Velocity
import com.zombies.game.core.Grid
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext

/**
 * 子弹移动系统 — 按 Velocity 推进 Transform，飞出 maxX 后销毁。
 *
 * M8：若传入 [ctx]，子弹推进后检测同行是否有火炬树桩（[Torchwood]）：
 *  - 若当前子弹中心 x 落在该火炬所在列中心 ± CELL_W*0.25 的范围内
 *  - 且 [Projectile.lastTorchedCol] 不等于该列号
 *  则把 damage ×= 火炬 multiplier，并更新 lastTorchedCol + fireEnhanced = true。
 *
 * 这避免了同一火炬重复加成，但允许同行不同火炬串联加成（2×2=4×、2×2×2=8× 等）。
 *
 * @param ctx 可选；不传则仅负责普通推进/销毁
 */
class ProjectileSystem(
    private val ctx: GameContext? = null
) : GameSystem(priority = 50) {

    override fun update(world: World, dtMs: Long) {
        val dtSec = dtMs / 1000f
        world.forEachWith<Projectile> { entity, proj ->
            val t = entity.get<Transform>() ?: return@forEachWith
            val v = entity.get<Velocity>() ?: return@forEachWith
            t.x += v.vx * dtSec
            t.y += v.vy * dtSec
            if (t.x > proj.maxX) {
                world.removeEntity(entity.id)
                return@forEachWith
            }
            // 穿越火炬树桩强化
            ctx?.let { applyTorchwood(world, it, proj, t.x) }
        }
    }

    private fun applyTorchwood(world: World, ctx: GameContext, proj: Projectile, bulletX: Float) {
        val row = proj.row
        // 只扫当前 bulletX 可能命中的列（周围 ±1）
        val centerCol = ((bulletX - Grid.ORIGIN_X) / Grid.CELL_W).toInt().coerceIn(0, Grid.COLS - 1)
        for (c in (centerCol - 1).coerceAtLeast(0)..(centerCol + 1).coerceAtMost(Grid.COLS - 1)) {
            val pid = ctx.occupancy.get(row, c)
            if (pid <= 0L) continue
            val plant = world.getEntity(pid) ?: continue
            val torch = plant.get<Torchwood>() ?: continue
            if (proj.lastTorchedCol == c) continue
            val cx = Grid.cellCenterX(c)
            val half = Grid.CELL_W * 0.25f
            if (bulletX in (cx - half)..(cx + half)) {
                proj.damage = (proj.damage * torch.multiplier).toInt()
                proj.fireEnhanced = true
                proj.lastTorchedCol = c
                return
            }
        }
    }
}
