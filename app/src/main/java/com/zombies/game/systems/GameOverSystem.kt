package com.zombies.game.systems

import com.zombies.game.audio.SoundId
import com.zombies.game.components.Transform
import com.zombies.game.components.ZombieTag
import com.zombies.game.core.Grid
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext
import com.zombies.game.save.ClearStats

/**
 * 胜负判定系统：
 *  - DEFEAT：任一僵尸 x <= HOUSE_LINE（即冲过草坪最左侧进入"房屋"）
 *  - VICTORY：关卡时间 > totalDurationMs 且所有波次已生成 且场上无僵尸
 *
 * 切换到非 RUNNING 状态时：
 *  - 播放胜/负音效
 *  - 触发 [GameContext.onVictory] / [GameContext.onDefeat] 回调（仅一次）
 * 之后停止工作。
 */
class GameOverSystem(private val ctx: GameContext) : GameSystem(priority = 90) {

    override fun update(world: World, dtMs: Long) {
        if (ctx.state != GameContext.State.RUNNING) return

        // 检查僵尸是否到家
        var zombieAlive = 0
        var defeated = false
        world.forEachWith2<ZombieTag, Transform> { _, _, t ->
            zombieAlive++
            if (t.x <= HOUSE_LINE) defeated = true
        }
        if (defeated) {
            ctx.state = GameContext.State.DEFEAT
            ctx.audio.play(SoundId.DEFEAT)
            ctx.onDefeat?.invoke()
            return
        }

        // 检查胜利
        val allSpawned = ctx.nextSpawnIndex >= ctx.wave.entries.size
        val timeUp = ctx.levelElapsedMs >= ctx.wave.totalDurationMs
        if (allSpawned && timeUp && zombieAlive == 0) {
            ctx.state = GameContext.State.VICTORY
            ctx.audio.play(SoundId.VICTORY)
            val stats = ClearStats(
                timeMs = ctx.levelElapsedMs,
                sunLeft = ctx.economy.sun
            )
            ctx.onVictory?.invoke(stats)
        }
    }

    companion object {
        /** 僵尸 x 低于此值视为"冲进屋子"（草坪左侧边界再左一点，给玩家反应余地） */
        val HOUSE_LINE: Float = Grid.ORIGIN_X - Grid.CELL_W * 0.25f
    }
}
