package com.zombies.game.systems

import com.zombies.game.components.GridCell
import com.zombies.game.components.Health
import com.zombies.game.components.PlantTag
import com.zombies.game.components.Projectile
import com.zombies.game.components.ProjectileTag
import com.zombies.game.components.Transform
import com.zombies.game.core.Grid
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext

/**
 * Boss "扔车" 专用系统：处理 [ProjectileTag] kind == "car" 的投射物。
 *
 * 行为：
 *  - 每帧扫描所有同行的植物，若车体横向范围（x ± CAR_HALF_W）覆盖到植物格中心 → 对植物造成 400 伤
 *    （通常一击必杀；即便是 4000HP 的坚果也直接殒命——设定如此，给 Boss 压迫感）
 *  - 车飞出左侧边界（x <= LEFT_BOUND）后销毁
 *  - 扔车不伤害僵尸（是 Boss 的盟友工具）
 *
 * 命中幂等：用 [Projectile.lastTorchedCol] 作为"本车已碾过的最远左列记录"（复用字段，避免多重命中）。
 *
 * priority = 52（早于 ProjectileCollision=55，独立处理；ProjectileCollision 按 ProjectileTag 过滤
 *              出 car 会自动忽略，因为 Projectile 组件没被特殊标记，但其"向左飞"语义与
 *              ProjectileCollisionSystem"同行最靠前"的规则不匹配。这里单独处理更清晰。）
 */
class BossCarSystem(
    private val ctx: GameContext? = null
) : GameSystem(priority = 52) {

    override fun update(world: World, dtMs: Long) {
        world.forEachWith<Projectile> { entity, proj ->
            val tag = entity.get<ProjectileTag>() ?: return@forEachWith
            if (tag.kind != "car") return@forEachWith
            val t = entity.get<Transform>() ?: return@forEachWith

            // 飞出左侧 → 销毁
            if (t.x <= LEFT_BOUND) {
                world.removeEntity(entity.id)
                return@forEachWith
            }

            // 扫同行植物；在 x ± CAR_HALF_W 范围内，未被该车处理过的 → 扣血
            world.forEachWith2<PlantTag, GridCell> { plant, _, cell ->
                if (cell.row != proj.row) return@forEachWith2
                val px = Grid.cellCenterX(cell.col)
                val dx = kotlin.math.abs(px - t.x)
                if (dx > CAR_HALF_W) return@forEachWith2
                // 用 lastTorchedCol 做幂等（每列只碾一次）
                if (proj.lastTorchedCol == cell.col) return@forEachWith2
                proj.lastTorchedCol = cell.col
                plant.get<Health>()?.damage(proj.damage)
            }
        }
    }

    companion object {
        /** 车身左右半宽；略大于单格，确保对中心列产生命中。 */
        const val CAR_HALF_W = 80f
        /** 车飞出草坪左侧这个 x 后销毁。 */
        val LEFT_BOUND: Float = Grid.ORIGIN_X - 200f
    }
}
