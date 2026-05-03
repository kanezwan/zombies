package com.zombies.game.systems

import com.zombies.game.components.StatusEffects
import com.zombies.game.components.Transform
import com.zombies.game.components.Velocity
import com.zombies.game.components.ZombieMover
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext

/**
 * 僵尸行走系统：根据 [ZombieMover.state] 决定是否推进 Transform。
 *  - WALKING：以 `baseSpeed * currentSlowMultiplier` 推进
 *  - EATING：原地不动（vx = 0）
 *
 * 每帧根据 StatusEffects 重新计算速度，不把减速值写回 baseSpeed，保证 debuff 到期能自动还原。
 *
 * priority = 45
 */
class ZombieMoveSystem(private val ctx: GameContext) : GameSystem(priority = 45) {

    override fun update(world: World, dtMs: Long) {
        if (ctx.state != GameContext.State.RUNNING) return
        val dtSec = dtMs / 1000f
        world.forEachWith2<ZombieMover, Transform> { entity, mover, t ->
            val v = entity.get<Velocity>() ?: return@forEachWith2
            when (mover.state) {
                ZombieMover.State.WALKING -> {
                    val slow = entity.get<StatusEffects>()?.currentSlowMultiplier() ?: 1f
                    val effective = mover.baseSpeed * slow
                    v.vx = -effective
                    t.x += v.vx * dtSec
                }
                ZombieMover.State.EATING -> {
                    v.vx = 0f
                }
            }
        }
    }
}
