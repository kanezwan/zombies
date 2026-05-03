package com.zombies.game.systems

import com.zombies.game.components.FloatingText
import com.zombies.game.components.Transform
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World

/**
 * 推进飘字生命周期：累加 elapsedMs，让 Transform.y 上浮，到期移除实体。
 *
 * priority = 2（在 EconomySystem=1 之后、PlantingSystem=0 之前的下一帧生效，
 * 实际上与 0/1 顺序无硬依赖，写 2 只是避免 0 这种容易混淆的值）。
 */
class FloatingTextSystem : GameSystem(priority = 2) {
    override fun update(world: World, dtMs: Long) {
        val dtSec = dtMs / 1000f
        val toRemove = ArrayList<Long>(4)
        world.forEachWith<FloatingText> { entity, ft ->
            ft.elapsedMs += dtMs
            entity.get<Transform>()?.let { t ->
                t.y += ft.vyPerSec * dtSec
            }
            if (ft.elapsedMs >= ft.totalMs) {
                toRemove.add(entity.id)
            }
        }
        for (id in toRemove) world.removeEntity(id)
    }
}
