package com.zombies.game.systems

import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.input.InputEvent
import com.zombies.game.input.InputQueue
import com.zombies.game.level.GameContext

/**
 * 卡牌槽点击系统：
 *  - 命中卡牌 → 切换 [GameContext.selectedPlantType]，吃掉事件
 *  - 命中铲子 → 切换 [GameContext.shovelMode]，吃掉事件
 *  - 选中卡牌时自动关闭铲子模式；开启铲子时自动取消卡牌
 *
 * 优先级 -10：在 PlantingSystem 前消化 HUD 点击。
 */
class CardTouchSystem(
    private val ctx: GameContext,
    private val rawQueue: InputQueue,
    private val forwardQueue: InputQueue
) : GameSystem(priority = -10) {

    override fun update(world: World, dtMs: Long) {
        val events = rawQueue.drain()
        val cards = ctx.availablePlants()
        for (ev in events) {
            if (ev is InputEvent.Down) {
                val idx = hitCardIndex(ev.virtualX, ev.virtualY, cards.size)
                if (idx >= 0) {
                    val type = cards[idx].type
                    ctx.selectedPlantType =
                        if (ctx.selectedPlantType == type) null else type
                    if (ctx.selectedPlantType != null) ctx.shovelMode = false
                    continue
                }
                if (hitShovel(ev.virtualX, ev.virtualY)) {
                    ctx.shovelMode = !ctx.shovelMode
                    if (ctx.shovelMode) ctx.selectedPlantType = null
                    continue
                }
            }
            forwardQueue.offer(ev)
        }
    }

    private fun hitCardIndex(x: Float, y: Float, count: Int): Int {
        for (i in 0 until count) {
            val r = CardHudRenderSystem.cardRect(i, count)
            if (x in r.left..r.right && y in r.top..r.bottom) return i
        }
        return -1
    }

    private fun hitShovel(x: Float, y: Float): Boolean {
        val r = CardHudRenderSystem.shovelRect()
        return x in r.left..r.right && y in r.top..r.bottom
    }
}
