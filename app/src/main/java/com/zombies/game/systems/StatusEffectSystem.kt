package com.zombies.game.systems

import com.zombies.game.components.Armor
import com.zombies.game.components.Boss
import com.zombies.game.components.Renderable
import com.zombies.game.components.StatusEffects
import com.zombies.game.components.ZombieTag
import com.zombies.game.components.ZombieMover
import com.zombies.game.ecs.Entity
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext

/**
 * 状态效果推进系统：
 *  - 推进所有 [StatusEffects] 的时间；到期的效果被移除
 *  - 对僵尸：根据"当前最强减速系数"刷新渲染颜色（被减速 → 蓝；正在啃食 → 维持吃食颜色不覆盖；否则还原本色）
 *
 * 不在本系统修改 Velocity —— MovementSystem 在 update 时每帧读取 StatusEffects.currentSlowMultiplier()
 * 作为速度系数，避免把"原速度"写入 Velocity 导致信息丢失。
 *
 * priority = 15（早于 Movement=45，晚于 Wave=3）
 */
class StatusEffectSystem(private val ctx: GameContext) : GameSystem(priority = 15) {

    override fun update(world: World, dtMs: Long) {
        if (ctx.state != GameContext.State.RUNNING) return

        world.forEachWith<StatusEffects> { entity, effects ->
            effects.tick(dtMs)
            refreshZombieColor(entity, effects)
        }
    }

    /**
     * 仅处理 WALKING 状态下的僵尸色：
     *  - 有减速效果：覆盖为 ZOMBIE_SLOWED
     *  - 无减速效果：还原本色（type 对应色）
     *
     * EATING 状态下的 ZOMBIE_EATING 颜色由 ZombieEatSystem 独立管理，本系统不覆盖。
     */
    private fun refreshZombieColor(entity: Entity, effects: StatusEffects) {
        val tag = entity.get<ZombieTag>() ?: return
        val mover = entity.get<ZombieMover>() ?: return
        if (mover.state == ZombieMover.State.EATING) return
        // Boss 免疫 slow/freeze 视觉覆盖：保留阶段色
        if (entity.get<Boss>() != null) return
        val renderable = entity.get<Renderable>() ?: return
        val frozen = effects.currentSlowMultiplier() == 0f
        val slowed = !frozen && effects.currentSlowMultiplier() < 1f
        val expected = when {
            frozen -> Renderable.ZOMBIE_FROZEN
            slowed -> Renderable.ZOMBIE_SLOWED
            else -> zombieBaseColor(tag, entity)
        }
        if (renderable.color != expected) {
            entity.add(
                Renderable(
                    shape = renderable.shape,
                    color = expected,
                    width = renderable.width,
                    height = renderable.height,
                    zOrder = renderable.zOrder
                )
            )
        }
    }

    /**
     * 还原本色（按 [ZombieTag.type]）。必须覆盖所有可能的僵尸类型，否则 slow debuff 到期
     * 后会错误地还原成 basic 色。
     *
     * 特殊：newspaper 已破甲（[Armor.broken]=true）时还原为暴怒红，而非初始灰蓝。
     */
    private fun zombieBaseColor(tag: ZombieTag, entity: Entity): Int = when (tag.type) {
        "conehead" -> Renderable.ZOMBIE_CONEHEAD
        "buckethead" -> Renderable.ZOMBIE_BUCKETHEAD
        "flag" -> Renderable.ZOMBIE_FLAG
        "polevault" -> Renderable.ZOMBIE_POLEVAULT
        "newspaper" ->
            if (entity.get<Armor>()?.broken == true) Renderable.ZOMBIE_NEWSPAPER_ENRAGED
            else Renderable.ZOMBIE_NEWSPAPER
        "football" -> Renderable.ZOMBIE_FOOTBALL
        "jester" -> Renderable.ZOMBIE_JESTER
        else -> Renderable.ZOMBIE_BASIC
    }
}
