package com.zombies.game.systems

import com.zombies.game.components.PlantTag
import com.zombies.game.components.Renderable
import com.zombies.game.components.Sleep
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext

/**
 * 夜间植物睡眠系统：
 *  - 白天（stage.skySunEnabled == true）→ 蘑菇类 [Sleep.asleep] = true，渲染变灰
 *  - 夜间（stage.skySunEnabled == false）→ 醒来，渲染还原
 *
 * Shooter / Producer 系统独立判断 Sleep 标记后跳过工作；此系统只负责状态 + 视觉切换。
 *
 * priority = 18（状态标记要在 ShooterSystem=40 / ProducerSystem=30 之前完成）
 *
 * 设计说明：为了还原色，在设置 asleep 时记录一次"醒着的颜色"快照（绑定在 [Sleep.wakeColor] 中）。
 * 但当前 Sleep 组件无此字段。简化做法：每帧检查 —— 若 asleep 设为 true，则把 Renderable.color 替换成
 * [Renderable.PLANT_ASLEEP]；从 sleep 切回 wake 时，根据 PlantTag.type 重新决定原色（已知的蘑菇类
 * 使用 [Renderable.PLANT_PUFFSHROOM]）。
 */
class NightPlantSleepSystem(
    private val ctx: GameContext
) : GameSystem(priority = 18) {

    override fun update(world: World, dtMs: Long) {
        val shouldSleep = ctx.stage.skySunEnabled // 白天 = 睡；夜间 = 醒
        world.forEachWith2<Sleep, PlantTag> { entity, sleep, tag ->
            if (sleep.asleep == shouldSleep) return@forEachWith2 // 无变化
            sleep.asleep = shouldSleep
            val r = entity.get<Renderable>() ?: return@forEachWith2
            val newColor = if (shouldSleep) {
                Renderable.PLANT_ASLEEP
            } else {
                colorByType(tag.type)
            }
            entity.add(
                Renderable(
                    shape = r.shape,
                    color = newColor,
                    width = r.width,
                    height = r.height,
                    zOrder = r.zOrder
                )
            )
        }
    }

    private fun colorByType(type: String): Int = when (type) {
        "puffshroom" -> Renderable.PLANT_PUFFSHROOM
        else -> Renderable.PLANT_SUNFLOWER // 未知蘑菇兜底
    }
}
