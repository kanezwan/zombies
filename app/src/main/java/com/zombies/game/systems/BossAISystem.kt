package com.zombies.game.systems

import com.zombies.game.components.Boss
import com.zombies.game.components.GridCell
import com.zombies.game.components.Health
import com.zombies.game.components.PlantFreeze
import com.zombies.game.components.PlantTag
import com.zombies.game.components.Renderable
import com.zombies.game.components.Transform
import com.zombies.game.components.ZombieTag
import com.zombies.game.config.ZombieConfig
import com.zombies.game.core.Grid
import com.zombies.game.ecs.Entity
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.entity.ProjectileFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import kotlin.random.Random

/**
 * Boss AI 系统（M11 僵王博士雏形）。
 *
 * 每帧对场上所有携带 [Boss] 组件的实体：
 *  1. 根据当前 HP 百分比切换 [Boss.phase]（1/2/3），并在跳变为 3 时触发一次性"冻结全场植物"
 *  2. 推进 [Boss.summonCooldownMs]：<=0 时召唤 2 只随机小怪（basic/conehead）到随机行
 *  3. 推进 [Boss.throwCarCooldownMs]：<=0 时选"植物最多的一行"扔车横扫
 *  4. 根据阶段切换 Boss 渲染色（可选）
 *
 * 扔车实现：创建 [com.zombies.game.components.Projectile]，从屏幕右侧超出位置出发，
 * 以高速向左飞；溅射半径 = 整行（splashRadius=1800）、伤害=400、ratio=1。
 * 由于扔车应碾压**植物**而非僵尸，使用独立 ProjectileTag("car")，
 * 在 [BossCarSystem] 里单独处理碰撞（而非复用 ProjectileCollisionSystem）。
 *
 * priority = 35（早于 Shooter=40 / Move=45；晚于 ZombieSpawn=3，确保 Boss 生成后当帧不立即技能）
 */
class BossAISystem(
    private val ctx: GameContext,
    private val random: Random = Random.Default
) : GameSystem(priority = 35) {

    override fun update(world: World, dtMs: Long) {
        if (ctx.state != GameContext.State.RUNNING) return

        world.forEachWith<Boss> { entity, boss ->
            val hp = entity.get<Health>() ?: return@forEachWith
            if (hp.isDead) return@forEachWith

            // ---------- 阶段切换 ----------
            val ratio = hp.hp.toFloat() / hp.maxHp.toFloat()
            val newPhase = when {
                ratio <= 0.33f -> 3
                ratio <= 0.66f -> 2
                else -> 1
            }
            if (newPhase > boss.phase) {
                boss.phase = newPhase
                onPhaseEnter(world, entity, boss)
            }

            // ---------- 召唤 ----------
            boss.summonCooldownMs -= dtMs
            if (boss.summonCooldownMs <= 0L) {
                boss.summonCooldownMs = boss.summonIntervalMs
                summonMinions(world)
            }

            // ---------- 扔车 ----------
            boss.throwCarCooldownMs -= dtMs
            if (boss.throwCarCooldownMs <= 0L) {
                boss.throwCarCooldownMs = boss.throwCarIntervalMs
                throwCar(world, entity)
            }
        }
    }

    /**
     * 进入新阶段时的一次性效果：
     *  - Phase 2：加速召唤（cooldown 减半）
     *  - Phase 3：冻结全场植物 5 秒
     */
    private fun onPhaseEnter(world: World, bossEntity: Entity, boss: Boss) {
        when (boss.phase) {
            2 -> {
                boss.summonCooldownMs = (boss.summonIntervalMs / 2).coerceAtLeast(1_000L)
                tintBoss(bossEntity, Renderable.ZOMBIE_BOSS_PHASE2)
            }
            3 -> {
                tintBoss(bossEntity, Renderable.ZOMBIE_BOSS_PHASE3)
                if (!boss.freezeAllTriggered) {
                    boss.freezeAllTriggered = true
                    freezeAllPlants(world, durationMs = 5_000L)
                }
            }
        }
    }

    private fun tintBoss(entity: Entity, color: Int) {
        val r = entity.get<Renderable>() ?: return
        entity.add(
            Renderable(
                shape = r.shape,
                color = color,
                width = r.width,
                height = r.height,
                zOrder = r.zOrder
            )
        )
    }

    /**
     * 召唤 2 只小怪到随机行（0~4）。小怪类型：basic 或 conehead（7:3）。
     * 复用 [ZombieFactory.create]——它会把僵尸刷在草坪右侧边界外。
     */
    private fun summonMinions(world: World) {
        val basicCfg = ctx.zombies["basic"] ?: ZombieConfig.BASIC
        val coneCfg = ctx.zombies["conehead"] ?: ZombieConfig.CONEHEAD
        repeat(2) {
            val cfg = if (random.nextFloat() < 0.7f) basicCfg else coneCfg
            val row = random.nextInt(Grid.ROWS)
            ZombieFactory.create(world, cfg, row)
        }
    }

    /**
     * 扔车：选"植物最多的一行"；若无植物则选 Boss 自身所在行。
     *
     * 扔车作为大伤害溅射投射物复用 [ProjectileFactory.createMelon]，
     * 但 splashRadius 非常大，确保横扫整行；方向为向左（负速度）。
     *
     * 为了避免扔车反复命中同一僵尸（Projectile 是同行）还能碾压植物，
     * 这里使用 [ProjectileFactory.createCar] 专用工厂。
     */
    private fun throwCar(world: World, bossEntity: Entity) {
        val bossRow = bossEntity.get<GridCell>()?.row ?: 2
        val targetRow = pickRowWithMostPlants(world, bossRow)
        // 从屏幕右侧外飞入，给玩家一点预警视觉
        val startX = Grid.ORIGIN_X + Grid.WIDTH + 100f
        val startY = Grid.cellCenterY(targetRow)
        ProjectileFactory.createCar(
            world = world,
            row = targetRow,
            startX = startX,
            startY = startY,
            speed = -720f,   // 向左飞（覆盖整行约 3 秒内）
            damage = 400
        )
    }

    private fun pickRowWithMostPlants(world: World, fallback: Int): Int {
        val counts = IntArray(Grid.ROWS)
        world.forEachWith2<PlantTag, GridCell> { _, _, cell ->
            if (cell.row in 0 until Grid.ROWS) counts[cell.row]++
        }
        var bestRow = fallback
        var bestCount = -1
        for (r in 0 until Grid.ROWS) {
            if (counts[r] > bestCount) {
                bestCount = counts[r]
                bestRow = r
            }
        }
        return if (bestCount <= 0) fallback else bestRow
    }

    /**
     * 冻结所有植物 [durationMs] 毫秒。
     *
     * 对每株植物：
     *  - 追加/刷新 [PlantFreeze] 组件（PlantFreezeSystem 每帧倒计时）
     */
    private fun freezeAllPlants(world: World, durationMs: Long) {
        world.forEachWith<PlantTag> { entity, _ ->
            val existing = entity.get<PlantFreeze>()
            if (existing == null) {
                entity.add(PlantFreeze(remainingMs = durationMs))
            } else if (existing.remainingMs < durationMs) {
                existing.remainingMs = durationMs
            }
        }
    }
}
