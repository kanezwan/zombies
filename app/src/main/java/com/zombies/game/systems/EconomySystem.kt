package com.zombies.game.systems

import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext

/**
 * 经济系统：推进卡牌冷却倒计时。
 * 阳光数变更由其它系统（ProducerSystem 产出 -> 拾取系统 pick up）驱动，这里仅做 tick。
 */
class EconomySystem(private val ctx: GameContext) : GameSystem(priority = 1) {
    override fun update(world: World, dtMs: Long) {
        ctx.economy.tick(dtMs)
    }
}
