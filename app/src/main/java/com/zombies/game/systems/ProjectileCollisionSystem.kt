package com.zombies.game.systems

import com.zombies.game.audio.SoundId
import com.zombies.game.components.Armor
import com.zombies.game.components.Boss
import com.zombies.game.components.GridCell
import com.zombies.game.components.Health
import com.zombies.game.components.Projectile
import com.zombies.game.components.ReflectShield
import com.zombies.game.components.Renderable
import com.zombies.game.components.StatusEffects
import com.zombies.game.components.Transform
import com.zombies.game.components.ZombieMover
import com.zombies.game.components.ZombieTag
import com.zombies.game.ecs.Entity
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext
import kotlin.math.abs

/**
 * 子弹 vs 同行僵尸的命中检测。
 *
 * 命中条件：同 row + |bulletX - zombieX| <= HIT_DIST
 *
 * 命中后处理顺序：
 *  1. 若僵尸携带 [ReflectShield] 且子弹**非减速、非火焰强化、非溅射** → 子弹向右飞出（通过把
 *     [Transform.x] 推到 [Projectile.maxX] 之外实现），同时关闭 shield。子弹不会造成任何伤害。
 *  2. 否则：有 [Armor] 先吸收伤害；溢出才进入 [Health]。
 *     - 护甲破碎帧：若该僵尸配置了暴走（[ZombieTag] 匹配 "newspaper" 且 [ZombieMover] 可写），
 *       立刻把 [ZombieMover.baseSpeed] ×enrageMul，并切换渲染为暴怒色。
 *  3. 附加减速（若子弹有 slow）
 *  4. **新增**：溅射——若子弹 [Projectile.hasSplash]，对同行、x 在 (bestX, bestX+splashRadius]
 *     的其他僵尸造成 ratio×damage 伤害（不传递 slow）。
 *  5. 播放命中音效；销毁子弹（反弹情况子弹保留）
 *
 * priority = 55
 */
class ProjectileCollisionSystem(
    private val ctx: GameContext? = null
) : GameSystem(priority = 55) {

    override fun update(world: World, dtMs: Long) {
        // 收集同行僵尸
        data class Z(
            val entity: Entity,
            val id: Long,
            val row: Int,
            val x: Float,
            val hp: Health,
            val armor: Armor?,
            val shield: ReflectShield?,
            val effects: StatusEffects?
        )
        val zombies = ArrayList<Z>(16)
        world.forEachWith2<ZombieTag, Transform> { entity, _, t ->
            val cell = entity.get<GridCell>() ?: return@forEachWith2
            val hp = entity.get<Health>() ?: return@forEachWith2
            if (hp.isDead) return@forEachWith2
            zombies.add(
                Z(
                    entity, entity.id, cell.row, t.x, hp,
                    entity.get<Armor>(),
                    entity.get<ReflectShield>(),
                    entity.get<StatusEffects>()
                )
            )
        }
        if (zombies.isEmpty()) return

        world.forEachWith<Projectile> { bullet, proj ->
            val bt = bullet.get<Transform>() ?: return@forEachWith
            // 取同行、最靠前（x 最小）的在命中范围内的僵尸
            var best: Z? = null
            for (z in zombies) {
                if (z.row != proj.row) continue
                if (z.hp.isDead) continue
                if (abs(z.x - bt.x) > HIT_DIST) continue
                if (best == null || z.x < best.x) best = z
            }
            if (best == null) return@forEachWith

            // ---------- 1. Jester 反弹（溅射豆不反弹，免得策略黑洞） ----------
            val shield = best.shield
            if (shield != null && shield.active &&
                !proj.hasSlow() && !proj.fireEnhanced && !proj.hasSplash()
            ) {
                reflectBullet(bullet, proj, bt)
                shield.active = false
                ctx?.audio?.play(SoundId.HIT)
                return@forEachWith
            }

            // ---------- 2. 伤害结算：先护甲后血量 ----------
            val incoming = proj.damage
            val bestArmor = best.armor
            val toBody = bestArmor?.absorb(incoming) ?: incoming
            // 护甲刚破碎 → 触发暴走
            if (bestArmor != null && bestArmor.broken) {
                onArmorBroken(best.entity)
            }
            if (toBody > 0) {
                best.hp.damage(toBody)
            }

            // ---------- 3. 减速附加（Boss 免疫） ----------
            if (proj.hasSlow() && best.entity.get<Boss>() == null) {
                best.effects?.addOrRefreshSlow(
                    source = proj.slowSource.ifEmpty { "slow" },
                    multiplier = proj.slowMultiplier,
                    durationMs = proj.slowDurationMs
                )
            }

            // ---------- 4. 溅射（仅对同行、位于主目标前方一定距离内的其他僵尸） ----------
            if (proj.hasSplash()) {
                val splashDmg = (proj.damage * proj.splashDamageRatio).toInt().coerceAtLeast(1)
                val primaryId = best.id
                val centerX = best.x
                for (z in zombies) {
                    if (z.id == primaryId) continue
                    if (z.row != proj.row) continue
                    if (z.hp.isDead) continue
                    val dx = z.x - centerX
                    // 只溅射前方（x 更大）或紧挨后方（保留 ±splashRadius 对称语义更直觉）
                    if (abs(dx) > proj.splashRadius) continue
                    val zArmor = z.armor
                    val finalDmg = zArmor?.absorb(splashDmg) ?: splashDmg
                    if (zArmor != null && zArmor.broken) {
                        onArmorBroken(z.entity)
                    }
                    if (finalDmg > 0) z.hp.damage(finalDmg)
                }
            }

            // ---------- 5. 音效 + 销毁子弹 ----------
            ctx?.audio?.play(SoundId.HIT)
            world.removeEntity(bullet.id)
        }
    }

    /**
     * 反弹子弹：把 Transform.x 推出 maxX，由 ProjectileSystem 在下一帧作为"飞出视口"清理。
     *
     * 视觉上玩家会看到"子弹瞬间消失"——等同于被小丑挡掉。后续可在 Projectile 组件增加 vx 字段做真反弹。
     */
    private fun reflectBullet(bullet: Entity, proj: Projectile, bt: Transform) {
        bt.x = proj.maxX + 1f
    }

    /**
     * 护甲破碎回调：
     * 对 Newspaper 类（ZombieTag.type == "newspaper"）触发暴走。
     */
    private fun onArmorBroken(entity: Entity) {
        val tag = entity.get<ZombieTag>() ?: return
        if (tag.type != "newspaper") return
        val mover = entity.get<ZombieMover>() ?: return
        val cfg = ctx?.zombies?.get("newspaper")
        val mul = cfg?.enragedSpeedMultiplier ?: 2.5f
        mover.baseSpeed = mover.baseSpeed * mul
        // 切换暴怒色
        entity.get<Renderable>()?.let { r ->
            entity.add(
                Renderable(
                    shape = r.shape,
                    color = Renderable.ZOMBIE_NEWSPAPER_ENRAGED,
                    width = r.width,
                    height = r.height,
                    zOrder = r.zOrder
                )
            )
        }
    }

    companion object {
        /** 命中半径（虚拟像素）。子弹半径 14 + 僵尸宽度半径 ~45 */
        const val HIT_DIST = 48f
    }
}
