package com.zombies.game.systems

import com.zombies.game.audio.SoundId
import com.zombies.game.components.BombFuse
import com.zombies.game.components.GridCell
import com.zombies.game.components.Health
import com.zombies.game.components.Renderable
import com.zombies.game.components.Transform
import com.zombies.game.components.ZombieTag
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext

/**
 * 炸弹引信系统：
 *  - 为所有带 [BombFuse] 的实体累积时间；到时触发一次范围伤害并标记为死亡（HealthSystem 清理）
 *  - 同时视觉变色（变成 EXPLOSION 颜色作为爆炸帧），HealthSystem 下一帧清理实体并释放格子
 *
 * priority = 35（在 Producer=30 之后，Shooter=40 之前，早于移动/啃食系统结算，避免本帧僵尸已走过边界）
 */
class BombSystem(private val ctx: GameContext) : GameSystem(priority = 35) {

    override fun update(world: World, dtMs: Long) {
        if (ctx.state != GameContext.State.RUNNING) return

        world.forEachWith<BombFuse> { entity, fuse ->
            if (fuse.triggered) return@forEachWith
            fuse.elapsedMs += dtMs
            if (fuse.elapsedMs < fuse.fuseMs) return@forEachWith

            fuse.triggered = true
            val t = entity.get<Transform>() ?: return@forEachWith

            ctx.audio.play(SoundId.EXPLODE)

            // 对半径内所有僵尸结算伤害
            val r2 = fuse.radius * fuse.radius
            world.forEachWith2<ZombieTag, Transform> { z, _, zt ->
                val zh = z.get<Health>() ?: return@forEachWith2
                if (zh.isDead) return@forEachWith2
                val dx = zt.x - t.x
                val dy = zt.y - t.y
                if (dx * dx + dy * dy <= r2) {
                    zh.damage(fuse.damage)
                }
            }

            // 视觉：爆炸帧（若实体已有 Renderable 则覆盖）
            if (entity.get<Renderable>() != null) {
                entity.add(
                    Renderable(
                        shape = Renderable.Shape.CIRCLE,
                        color = Renderable.EXPLOSION,
                        width = fuse.radius * 2f,
                        height = fuse.radius * 2f,
                        zOrder = 80 // 高于僵尸层
                    )
                )
            }

            // 自毁：给自身扣满血 → HealthSystem 下一帧清理 + 释放格子
            entity.get<Health>()?.let { h -> h.damage(h.hp) }

            // 保险：如果没 Health（不应出现），直接移除
            if (entity.get<Health>() == null) {
                val cell = entity.get<GridCell>()
                if (cell != null) ctx.occupancy.clear(cell.row, cell.col)
                world.removeEntity(entity.id)
            }
        }
    }
}
