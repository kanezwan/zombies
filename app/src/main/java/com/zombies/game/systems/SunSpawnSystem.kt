package com.zombies.game.systems

import com.zombies.game.core.Timer
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.entity.SunFactory
import com.zombies.game.level.GameContext

/**
 * 自然阳光掉落系统：每 [intervalMs] 产一颗从天而降的阳光。
 *
 * M9：若传入 [ctx] 且 [GameContext.stage].skySunEnabled == false（夜间），
 * 则关闭天空阳光。老的构造签名保持兼容。
 */
class SunSpawnSystem(
    private val virtualWidth: Float,
    intervalMs: Long = 10_000L,
    firstDelayMs: Long = 5_000L,
    private val ctx: GameContext? = null
) : GameSystem(priority = 5) {

    private val timer = Timer(intervalMs)
    private var firstDelayLeft = firstDelayMs

    override fun update(world: World, dtMs: Long) {
        // 夜间：不掉阳光（但仍累加 firstDelay 避免切回白天后突然刷一大堆）
        if (ctx != null && !ctx.stage.skySunEnabled) return

        if (firstDelayLeft > 0) {
            firstDelayLeft -= dtMs
            return
        }
        if (timer.tick(dtMs)) {
            SunFactory.createSky(world, virtualWidth)
        }
    }
}
