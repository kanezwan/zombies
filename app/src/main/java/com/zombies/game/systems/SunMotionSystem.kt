package com.zombies.game.systems

import com.zombies.game.components.Sun
import com.zombies.game.components.Transform
import com.zombies.game.components.Velocity
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World

/**
 * 阳光下落 + 寿命管理：
 *  - 下落到 targetY 后停止
 *  - 寿命归零自动消失（移除实体）
 */
class SunMotionSystem : GameSystem(priority = 20) {

    override fun update(world: World, dtMs: Long) {
        val dtSec = dtMs / 1000f
        world.forEachWith<Sun> { entity, sun ->
            val t = entity.get<Transform>() ?: return@forEachWith

            // 下落到目标 Y 后停止
            if (t.y < sun.targetY) {
                val v = entity.get<Velocity>()
                if (v != null) {
                    t.y += v.vy * dtSec
                    if (t.y >= sun.targetY) {
                        t.y = sun.targetY
                        v.vy = 0f
                    }
                }
            }

            // 到达后开始计寿命
            if (t.y >= sun.targetY) {
                sun.lifetimeMs -= dtMs
                if (sun.lifetimeMs <= 0) world.removeEntity(entity.id)
            }
        }
    }
}
