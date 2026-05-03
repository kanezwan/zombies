package com.zombies.game.systems

import com.zombies.game.components.PlantFreeze
import com.zombies.game.components.PlantTag
import com.zombies.game.components.Renderable
import com.zombies.game.ecs.Entity
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World

/**
 * 植物冻结系统（M11：Boss Phase 3 技能）。
 *
 * 每帧对带 [PlantFreeze] 的植物：
 *  - 倒计时 remainingMs
 *  - 若首次发现（刚挂上组件而 Renderable 还没变色），覆盖为 [Renderable.PLANT_FROZEN]，并记忆原色到
 *    组件的 wakeColor 字段——但当前 PlantFreeze 不持有 wakeColor，退而求其次：
 *    记录 colorByType + 当前 shape/wh 再切回。
 *  - 到期：移除 [PlantFreeze]，按 [PlantTag.type] 还原原色
 *
 * ShooterSystem / ProducerSystem 需另外识别 PlantFreeze 并跳过工作（见各自修改）。
 *
 * priority = 17（早于 Shooter=40 / Producer=30；晚于 NightPlantSleep=18？这里选 17 让冻结优先于
 *              睡眠判定，避免夜间植物被冻结时同时被"醒来"还原色）
 */
class PlantFreezeSystem : GameSystem(priority = 17) {

    /** 跟踪已应用冻结渲染的实体 id（避免重复写入组件导致每帧 GC） */
    private val tinted = HashSet<Long>()

    override fun update(world: World, dtMs: Long) {
        val toRestore = ArrayList<Entity>(8)
        world.forEachWith<PlantFreeze> { entity, fz ->
            fz.remainingMs -= dtMs
            if (fz.remainingMs <= 0L) {
                toRestore.add(entity)
            } else if (entity.id !in tinted) {
                tintFrozen(entity)
                tinted.add(entity.id)
            }
        }
        for (e in toRestore) {
            e.remove<PlantFreeze>()
            restoreColor(e)
            tinted.remove(e.id)
        }
    }

    private fun tintFrozen(entity: Entity) {
        val r = entity.get<Renderable>() ?: return
        entity.add(
            Renderable(
                shape = r.shape,
                color = Renderable.PLANT_FROZEN,
                width = r.width,
                height = r.height,
                zOrder = r.zOrder
            )
        )
    }

    private fun restoreColor(entity: Entity) {
        val tag = entity.get<PlantTag>() ?: return
        val r = entity.get<Renderable>() ?: return
        val newColor = colorByType(tag.type)
        if (r.color == newColor) return
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

    /** 植物类型 → 渲染色（覆盖所有已知植物）。未知类型返回向日葵色作为兜底。 */
    private fun colorByType(type: String): Int = when (type) {
        "sunflower" -> Renderable.PLANT_SUNFLOWER
        "peashooter" -> Renderable.PLANT_PEASHOOTER
        "repeater" -> Renderable.PLANT_REPEATER
        "snowpea" -> Renderable.PLANT_SNOWPEA
        "wallnut" -> Renderable.PLANT_WALLNUT
        "tallnut" -> Renderable.PLANT_TALLNUT
        "pumpkin" -> Renderable.PLANT_PUMPKIN
        "cherry_bomb", "cherrybomb" -> Renderable.PLANT_CHERRY
        "torchwood" -> Renderable.PLANT_TORCHWOOD
        "puffshroom" -> Renderable.PLANT_PUFFSHROOM
        "wintermelon" -> Renderable.PLANT_WINTERMELON
        else -> Renderable.PLANT_SUNFLOWER
    }
}
