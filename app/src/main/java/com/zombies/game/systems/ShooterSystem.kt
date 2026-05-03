package com.zombies.game.systems

import com.zombies.game.components.GridCell
import com.zombies.game.components.PlantFreeze
import com.zombies.game.components.Shooter
import com.zombies.game.components.Sleep
import com.zombies.game.components.Transform
import com.zombies.game.components.ZombieTag
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.entity.ProjectileFactory

/**
 * 射手攻击系统：
 *  - 常规豌豆射手：[Shooter.intervalMs] 间隔射 1 发
 *  - 双发豌豆（repeater）：每次触发排队补发 [Shooter.shotsPerFire] - 1 发，
 *    补发间隔为 [Shooter.shotGapMs]
 *  - 寒冰射手（snowpea）：子弹附带减速（slowMultiplier < 1 + slowDurationMs > 0）
 *
 * 仅在本行有僵尸时攒 CD（经典 PVZ 规则），同一 tick 会尝试补发等待中的"余发"。
 *
 * priority = 40
 */
class ShooterSystem(
    private val shouldCheckZombie: Boolean = true
) : GameSystem(priority = 40) {

    override fun update(world: World, dtMs: Long) {
        world.forEachWith2<Shooter, Transform> { entity, shooter, t ->
            // 睡眠中（夜间植物在白天）不射击
            if (entity.get<Sleep>()?.asleep == true) {
                shooter.elapsedMs = 0L
                return@forEachWith2
            }
            // 被 Boss 冻结的植物不射击
            if (entity.has<PlantFreeze>()) {
                shooter.elapsedMs = 0L
                return@forEachWith2
            }
            val cell = entity.get<GridCell>() ?: return@forEachWith2
            val row = cell.row

            val hasTarget =
                if (shouldCheckZombie && shooter.onlyWhenZombieInRow) hasZombieInRow(world, row)
                else true

            // ---- 先处理"排队中的补发"（即使此刻无目标也要把已排队的射完，避免状态泄漏）----
            if (shooter.pendingShots > 0) {
                shooter.nextShotGapMs -= dtMs
                while (shooter.pendingShots > 0 && shooter.nextShotGapMs <= 0L) {
                    fire(world, shooter, t, row)
                    shooter.pendingShots--
                    shooter.nextShotGapMs += shooter.shotGapMs
                }
            }

            if (!hasTarget) {
                shooter.elapsedMs = 0L
                return@forEachWith2
            }

            shooter.elapsedMs += dtMs
            if (shooter.elapsedMs >= shooter.intervalMs) {
                shooter.elapsedMs -= shooter.intervalMs
                // 立刻发第一发
                fire(world, shooter, t, row)
                // 其余的排队
                val rest = (shooter.shotsPerFire - 1).coerceAtLeast(0)
                if (rest > 0) {
                    shooter.pendingShots = rest
                    shooter.nextShotGapMs = shooter.shotGapMs
                }
            }
        }
    }

    private fun fire(world: World, shooter: Shooter, t: Transform, row: Int) {
        val startX = t.x + 20f
        val startY = t.y - 40f
        when (shooter.bulletKind) {
            "melon" -> ProjectileFactory.createMelon(
                world = world,
                row = row,
                startX = startX,
                startY = startY,
                speed = shooter.projectileSpeed,
                damage = shooter.damage,
                slowMultiplier = shooter.slowMultiplier,
                slowDurationMs = shooter.slowDurationMs,
                splashRadius = shooter.splashRadius,
                splashDamageRatio = shooter.splashDamageRatio
            )
            "icepea" -> ProjectileFactory.createIcePea(
                world = world,
                row = row,
                startX = startX,
                startY = startY,
                speed = shooter.projectileSpeed,
                damage = shooter.damage,
                slowMultiplier = shooter.slowMultiplier,
                slowDurationMs = shooter.slowDurationMs,
                slowSource = shooter.slowSource.ifEmpty { "snowpea" }
            )
            else -> {
                // 兼容旧调用：slow 字段非空则仍走 icepea；否则普通豌豆
                if (shooter.slowMultiplier > 0f && shooter.slowMultiplier < 1f && shooter.slowDurationMs > 0L) {
                    ProjectileFactory.createIcePea(
                        world = world,
                        row = row,
                        startX = startX,
                        startY = startY,
                        speed = shooter.projectileSpeed,
                        damage = shooter.damage,
                        slowMultiplier = shooter.slowMultiplier,
                        slowDurationMs = shooter.slowDurationMs,
                        slowSource = shooter.slowSource.ifEmpty { "snowpea" }
                    )
                } else {
                    ProjectileFactory.createPea(
                        world = world,
                        row = row,
                        startX = startX,
                        startY = startY,
                        speed = shooter.projectileSpeed,
                        damage = shooter.damage
                    )
                }
            }
        }
    }

    private fun hasZombieInRow(world: World, row: Int): Boolean {
        var found = false
        world.forEachWith2<ZombieTag, GridCell> { _, _, cell ->
            if (cell.row == row) found = true
        }
        return found
    }
}
