package com.zombies.game.systems

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.zombies.game.components.Boss
import com.zombies.game.components.Health
import com.zombies.game.core.Grid
import com.zombies.game.ecs.RenderSystem
import com.zombies.game.ecs.World

/**
 * Boss HP HUD：当场上存在至少一个带 [Boss] 组件的实体时，
 * 在草坪顶部绘制一条宽血条 + "BOSS Phase N  hp/maxHp (XX%)" 文字。
 *
 * 位置：底部（草坪下方），避免与 CardHud 重叠。
 * priority = 150（晚于 CardHud=100、早于 GameOver=200）
 *
 * 实现说明：若场上有多个 Boss，展示 HP 最低者（主要场景只有一个）。
 */
class BossHudRenderSystem : RenderSystem(priority = 150) {

    private val bgPaint = Paint().apply { color = Color.argb(200, 30, 30, 30) }
    private val fillPaint = Paint().apply { color = Color.rgb(229, 57, 53); isAntiAlias = true }
    private val borderPaint = Paint().apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 4f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE; textSize = 30f; isAntiAlias = true; isFakeBoldText = true
    }

    override fun render(world: World, canvas: Canvas) {
        val target = pickTargetBoss(world) ?: return
        val hp = target.health
        val boss = target.boss

        // 血条位置：草坪下方中央，宽 1200，高 28
        val centerX = Grid.ORIGIN_X + Grid.WIDTH / 2f
        val barW = BAR_WIDTH
        val barH = BAR_HEIGHT
        val left = centerX - barW / 2f
        val top = Grid.ORIGIN_Y + Grid.HEIGHT + 16f
        val rect = RectF(left, top, left + barW, top + barH)

        canvas.drawRect(rect, bgPaint)
        val ratio = (hp.hp.toFloat() / hp.maxHp.toFloat()).coerceIn(0f, 1f)
        val fillRight = left + barW * ratio
        canvas.drawRect(left, top, fillRight, top + barH, fillPaint)
        canvas.drawRect(rect, borderPaint)

        val pct = (ratio * 100).toInt()
        val txt = "Dr. Zomboss  Phase ${boss.phase}   ${hp.hp}/${hp.maxHp}  ($pct%)"
        canvas.drawText(txt, left + 20f, top + barH - 6f, textPaint)
    }

    companion object {
        const val BAR_WIDTH = 1200f
        const val BAR_HEIGHT = 32f

        /**
         * 纯函数筛选：从 [world] 里选出 HP 最低的未阵亡 Boss。
         * 返回 [BossRenderTarget] 或 null（无 Boss / 全部已死）。
         *
         * 独立出来便于单测。
         */
        internal fun pickTargetBoss(world: World): BossRenderTarget? {
            var bossHp: Health? = null
            var bossComp: Boss? = null
            world.forEachWith<Boss> { entity, comp ->
                val hp = entity.get<Health>() ?: return@forEachWith
                if (hp.isDead) return@forEachWith
                if (bossHp == null || hp.hp < (bossHp?.hp ?: Int.MAX_VALUE)) {
                    bossHp = hp
                    bossComp = comp
                }
            }
            val hp = bossHp ?: return null
            val boss = bossComp ?: return null
            return BossRenderTarget(hp, boss)
        }
    }

    /** 渲染目标的简单容器，避免依赖 Pair 的 IDE 推断问题。 */
    internal data class BossRenderTarget(val health: Health, val boss: Boss)
}
