package com.zombies.game.systems

import com.zombies.game.components.GridCell
import com.zombies.game.components.Health
import com.zombies.game.components.PlantTag
import com.zombies.game.components.ZombieTag
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext

/**
 * 通用血量 → 死亡系统：实体 HP <= 0 时移除；若是植物还需清空格子占用表。
 * 对僵尸、子弹、植物等所有带 [Health] 的实体都适用。
 *
 * 额外职责：
 *  - 僵尸死亡时递增 [GameContext.killsThisRun]（本局统计），由 UI 层在结算时一次性上报 SaveManager。
 */
class HealthSystem(private val ctx: GameContext) : GameSystem(priority = 80) {

    override fun update(world: World, dtMs: Long) {
        world.forEachWith<Health> { entity, health ->
            if (!health.isDead) return@forEachWith

            if (entity.has<PlantTag>()) {
                val cell = entity.get<GridCell>()
                if (cell != null) ctx.occupancy.clear(cell.row, cell.col)
            }
            if (entity.has<ZombieTag>()) {
                ctx.killsThisRun++
            }
            world.removeEntity(entity.id)
        }
    }
}
