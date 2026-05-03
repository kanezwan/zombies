package com.zombies.game.systems

import com.zombies.game.config.ZombieConfig
import com.zombies.game.core.Grid
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import kotlin.random.Random

/**
 * 按 [com.zombies.game.config.WaveConfig] 的时间表生成僵尸。
 * 仅在游戏处于 RUNNING 状态下累计时间与生成。
 */
class ZombieSpawnSystem(
    private val ctx: GameContext,
    private val random: Random = Random.Default
) : GameSystem(priority = 3) {

    override fun update(world: World, dtMs: Long) {
        if (ctx.state != GameContext.State.RUNNING) return
        ctx.levelElapsedMs += dtMs

        val entries = ctx.wave.entries
        while (ctx.nextSpawnIndex < entries.size) {
            val entry = entries[ctx.nextSpawnIndex]
            if (ctx.levelElapsedMs < entry.atMs) break
            val row = if (entry.row in 0 until Grid.ROWS) entry.row else random.nextInt(Grid.ROWS)
            val cfg = ctx.zombies[entry.zombieType]
                ?: ctx.zombies.values.firstOrNull()
                ?: ZombieConfig.BASIC
            ZombieFactory.create(world, cfg, row)
            ctx.nextSpawnIndex++
        }
    }
}
