package com.zombies.game.entity

import com.zombies.game.components.Armor
import com.zombies.game.components.Boss
import com.zombies.game.components.GridCell
import com.zombies.game.components.Health
import com.zombies.game.components.PoleVault
import com.zombies.game.components.ReflectShield
import com.zombies.game.components.Renderable
import com.zombies.game.components.StaticSpriteRenderable
import com.zombies.game.components.StatusEffects
import com.zombies.game.components.Transform
import com.zombies.game.components.Velocity
import com.zombies.game.components.WalkBob
import com.zombies.game.components.ZombieEater
import com.zombies.game.components.ZombieMover
import com.zombies.game.components.ZombieTag
import com.zombies.game.config.ZombieConfig
import com.zombies.game.core.Grid
import com.zombies.game.ecs.Entity
import com.zombies.game.ecs.World
import kotlin.random.Random

/**
 * 僵尸工厂：在草坪右侧刷出僵尸，向左行走。
 * 僵尸的 col 始终为 -1，位置以 [Transform] 为准；行 row 固定，碰撞检测按行进行。
 *
 * 支持类型：basic / conehead / buckethead / flag / polevault
 */
object ZombieFactory {

    fun create(world: World, cfg: ZombieConfig, row: Int): Entity {
        val e = world.createEntity()
        val startX = Grid.ORIGIN_X + Grid.WIDTH + Grid.CELL_W * 0.4f
        val startY = Grid.cellCenterY(row) + Grid.CELL_H * 0.35f
        e.add(Transform(startX, startY))
        e.add(GridCell(row, -1))
        e.add(Velocity(vx = -cfg.speed, vy = 0f))
        e.add(Health(cfg.hp))
        e.add(ZombieTag(cfg.type))
        e.add(ZombieMover(baseSpeed = cfg.speed))
        e.add(ZombieEater(intervalMs = cfg.attackIntervalMs, damage = cfg.attackDamage))
        e.add(StatusEffects())
        if (cfg.type == "polevault" && cfg.jumpDistance > 0f) {
            e.add(PoleVault(jumpDistance = cfg.jumpDistance))
        }
        if (cfg.armorHp > 0) {
            e.add(Armor(hp = cfg.armorHp, maxHp = cfg.armorHp))
        }
        if (cfg.reflectProjectiles) {
            e.add(ReflectShield(active = true))
        }
        if (cfg.isBoss) {
            e.add(Boss())
        }
        // Boss 体积更大；普通僵尸占满一格。
        val sizeMul = if (cfg.isBoss) 1.8f else 1.0f
        val heightMul = if (cfg.isBoss) 1.4f else 1.0f
        e.add(
            Renderable(
                shape = Renderable.Shape.RECT,
                color = colorOf(cfg.type),
                width = Grid.CELL_W * sizeMul,
                height = Grid.CELL_H * heightMul,
                zOrder = 50 + row
            )
        )

        // M9: 附加单张静态 PNG 渲染组件。
        //   - 约定 key = "sprites/zombies/zombie_<type>"；资源缺失时静默跳过
        //   - 尺寸比色块略大：Boss 用 2× 格子，普通僵尸 1.1× 宽 × 1.4× 高
        staticSpriteKeyFor(cfg.type)?.let { key ->
            val w = if (cfg.isBoss) Grid.CELL_W * 2.0f else Grid.CELL_W * 1.1f
            val h = if (cfg.isBoss) Grid.CELL_H * 2.0f else Grid.CELL_H * 1.4f
            e.add(
                StaticSpriteRenderable(
                    spriteKey = key,
                    widthPx = w,
                    heightPx = h,
                    zOrder = 51 + row
                )
            )
            // 走路抖动（伪帧动画）：
            //   - 随机相位 0~1000ms，让同批僵尸不同步，避免"齐步走"整齐划一的违和感
            //   - Boss 的步频更慢、幅度更大，普通僵尸快一些
            val isBoss = cfg.isBoss
            e.add(
                WalkBob(
                    amplitudePx = if (isBoss) 10f else 6f,
                    freqHz = if (isBoss) 1.8f else 2.8f,
                    tiltDeg = if (isBoss) 2f else 3f,
                    phaseMs = Random.nextLong(0, 1000)
                )
            )
        }
        return e
    }

    /**
     * 僵尸类型 → 静态图 key。未提供美术资源的类型返回 null。
     * 资源缺失时 StaticSpriteRenderSystem 会静默跳过，[Renderable] 色块继续兜底。
     */
    private fun staticSpriteKeyFor(type: String): String? = when (type) {
        "basic" -> "sprites/zombies/zombie_basic"
        "conehead" -> "sprites/zombies/zombie_conehead"
        "buckethead" -> "sprites/zombies/zombie_buckethead"
        "flag" -> "sprites/zombies/zombie_flag"
        "bossdrzomboss" -> "sprites/zombies/zombie_boss"
        else -> null
    }

    private fun colorOf(type: String): Int = when (type) {
        "conehead" -> Renderable.ZOMBIE_CONEHEAD
        "buckethead" -> Renderable.ZOMBIE_BUCKETHEAD
        "flag" -> Renderable.ZOMBIE_FLAG
        "polevault" -> Renderable.ZOMBIE_POLEVAULT
        "newspaper" -> Renderable.ZOMBIE_NEWSPAPER
        "football" -> Renderable.ZOMBIE_FOOTBALL
        "jester" -> Renderable.ZOMBIE_JESTER
        "bossdrzomboss" -> Renderable.ZOMBIE_BOSS
        else -> Renderable.ZOMBIE_BASIC
    }
}
