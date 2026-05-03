package com.zombies.game.systems

import com.zombies.game.components.PlantFreeze
import com.zombies.game.components.Producer
import com.zombies.game.components.Sleep
import com.zombies.game.components.Transform
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.entity.SunFactory
import com.zombies.game.level.GameContext

/**
 * 向日葵产阳光系统：累积时间到 [Producer.intervalMs] 后生成一颗阳光。
 * 开头有 [Producer.startupDelayMs] 延迟避免刚种下立刻产出。
 *
 * M9：支持场景产率倍率 —— 夜间默认 ×1.2 作为"没天空阳光"的补偿。
 */
class ProducerSystem(
    private val ctx: GameContext? = null
) : GameSystem(priority = 30) {

    override fun update(world: World, dtMs: Long) {
        val mul = ctx?.stage?.producerRateMultiplier ?: 1f
        val effectiveDt = (dtMs * mul).toLong()
        world.forEachWith2<Producer, Transform> { entity, p, t ->
            if (entity.get<Sleep>()?.asleep == true) return@forEachWith2
            if (entity.has<PlantFreeze>()) return@forEachWith2
            if (p.startupDelayMs > 0) {
                p.startupDelayMs -= effectiveDt
                return@forEachWith2
            }
            p.elapsedMs += effectiveDt
            if (p.elapsedMs >= p.intervalMs) {
                p.elapsedMs -= p.intervalMs
                SunFactory.createFromSunflower(world, t.x, t.y - 40f, p.amount)
            }
        }
    }
}
