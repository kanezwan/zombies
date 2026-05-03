package com.zombies.game.entity

import com.zombies.game.components.BombFuse
import com.zombies.game.components.GridCell
import com.zombies.game.components.Health
import com.zombies.game.components.PlantTag
import com.zombies.game.components.PoleVaultImmune
import com.zombies.game.components.Producer
import com.zombies.game.components.Renderable
import com.zombies.game.components.Shooter
import com.zombies.game.components.Sleep
import com.zombies.game.components.SpriteRenderable
import com.zombies.game.components.StaticSpriteRenderable
import com.zombies.game.components.Torchwood
import com.zombies.game.components.Transform
import com.zombies.game.config.PlantConfig
import com.zombies.game.core.Grid
import com.zombies.game.ecs.Entity
import com.zombies.game.ecs.World

/**
 * 植物工厂：根据 [PlantConfig] 在指定网格 (row, col) 生成 [Entity]。
 * 返回的 entity 已加入 [world] 的待添加队列，下一帧生效。
 *
 * 支持类型：sunflower / peashooter / repeater / snowpea /
 *           wallnut / tallnut / pumpkin / cherrybomb / torchwood
 * 调用方需自行检查格子是否空闲、阳光是否足够，并同步 GridOccupancy。
 */
object PlantFactory {

    fun create(world: World, cfg: PlantConfig, row: Int, col: Int): Entity {
        val e = world.createEntity()
        val cx = Grid.cellCenterX(col)
        val cy = Grid.cellCenterY(row) + Grid.CELL_H * 0.35f // 脚踩点靠下
        e.add(Transform(cx, cy))
        e.add(GridCell(row, col))
        e.add(Health(cfg.hp))
        e.add(PlantTag(cfg.type))

        when (cfg.type) {
            "sunflower" -> {
                e.add(
                    Producer(
                        intervalMs = cfg.produceIntervalMs,
                        amount = cfg.produceAmount,
                        startupDelayMs = 6_000L
                    )
                )
                e.add(
                    Renderable(
                        shape = Renderable.Shape.CIRCLE,
                        color = Renderable.PLANT_SUNFLOWER,
                        width = Grid.CELL_W * 0.6f,
                        height = Grid.CELL_W * 0.6f,
                        zOrder = 10 + row
                    )
                )
            }
            "peashooter" -> {
                e.add(
                    Shooter(
                        intervalMs = cfg.attackIntervalMs,
                        damage = cfg.damage,
                        projectileSpeed = cfg.projectileSpeed
                    )
                )
                e.add(
                    Renderable(
                        shape = Renderable.Shape.RECT,
                        color = Renderable.PLANT_PEASHOOTER,
                        width = Grid.CELL_W * 0.55f,
                        height = Grid.CELL_H * 0.75f,
                        zOrder = 10 + row
                    )
                )
            }
            "repeater" -> {
                e.add(
                    Shooter(
                        intervalMs = cfg.attackIntervalMs,
                        damage = cfg.damage,
                        projectileSpeed = cfg.projectileSpeed,
                        shotsPerFire = cfg.shotsPerFire,
                        shotGapMs = cfg.shotGapMs
                    )
                )
                e.add(
                    Renderable(
                        shape = Renderable.Shape.RECT,
                        color = Renderable.PLANT_REPEATER,
                        width = Grid.CELL_W * 0.6f,
                        height = Grid.CELL_H * 0.78f,
                        zOrder = 10 + row
                    )
                )
            }
            "snowpea" -> {
                e.add(
                    Shooter(
                        intervalMs = cfg.attackIntervalMs,
                        damage = cfg.damage,
                        projectileSpeed = cfg.projectileSpeed,
                        shotsPerFire = 1,
                        slowMultiplier = cfg.slowMultiplier,
                        slowDurationMs = cfg.slowDurationMs,
                        slowSource = "snowpea",
                        bulletKind = "icepea"
                    )
                )
                e.add(
                    Renderable(
                        shape = Renderable.Shape.RECT,
                        color = Renderable.PLANT_SNOWPEA,
                        width = Grid.CELL_W * 0.55f,
                        height = Grid.CELL_H * 0.75f,
                        zOrder = 10 + row
                    )
                )
            }
            "wallnut" -> {
                e.add(
                    Renderable(
                        shape = Renderable.Shape.CIRCLE,
                        color = Renderable.PLANT_WALLNUT,
                        width = Grid.CELL_W * 0.78f,
                        height = Grid.CELL_W * 0.78f,
                        zOrder = 10 + row
                    )
                )
            }
            "tallnut" -> {
                e.add(PoleVaultImmune.INSTANCE)
                e.add(
                    Renderable(
                        shape = Renderable.Shape.RECT,
                        color = Renderable.PLANT_TALLNUT,
                        width = Grid.CELL_W * 0.72f,
                        height = Grid.CELL_H * 0.95f, // 加高：视觉上表达"撑杆跳不过"
                        zOrder = 10 + row
                    )
                )
            }
            "pumpkin" -> {
                e.add(PoleVaultImmune.INSTANCE)
                e.add(
                    Renderable(
                        shape = Renderable.Shape.CIRCLE,
                        color = Renderable.PLANT_PUMPKIN,
                        width = Grid.CELL_W * 0.85f,
                        height = Grid.CELL_W * 0.85f,
                        zOrder = 10 + row
                    )
                )
            }
            "cherrybomb" -> {
                e.add(
                    BombFuse(
                        fuseMs = cfg.fuseMs,
                        radius = cfg.explodeRadius,
                        damage = cfg.explodeDamage
                    )
                )
                e.add(
                    Renderable(
                        shape = Renderable.Shape.CIRCLE,
                        color = Renderable.PLANT_CHERRY,
                        width = Grid.CELL_W * 0.72f,
                        height = Grid.CELL_W * 0.72f,
                        zOrder = 10 + row
                    )
                )
            }
            "torchwood" -> {
                e.add(Torchwood(multiplier = cfg.torchMultiplier))
                e.add(
                    Renderable(
                        shape = Renderable.Shape.RECT,
                        color = Renderable.PLANT_TORCHWOOD,
                        width = Grid.CELL_W * 0.55f,
                        height = Grid.CELL_H * 0.7f,
                        zOrder = 10 + row
                    )
                )
            }
            "puffshroom" -> {
                e.add(
                    Shooter(
                        intervalMs = cfg.attackIntervalMs,
                        damage = cfg.damage,
                        projectileSpeed = cfg.projectileSpeed
                    )
                )
                e.add(Sleep(asleep = false)) // 默认醒着；SleepSystem 按场景切换
                e.add(
                    Renderable(
                        shape = Renderable.Shape.CIRCLE,
                        color = Renderable.PLANT_PUFFSHROOM,
                        width = Grid.CELL_W * 0.55f,
                        height = Grid.CELL_W * 0.55f,
                        zOrder = 10 + row
                    )
                )
            }
            "wintermelon" -> {
                e.add(
                    Shooter(
                        intervalMs = cfg.attackIntervalMs,
                        damage = cfg.damage,
                        projectileSpeed = cfg.projectileSpeed,
                        slowMultiplier = cfg.slowMultiplier,
                        slowDurationMs = cfg.slowDurationMs,
                        slowSource = "wintermelon",
                        splashRadius = cfg.splashRadius,
                        splashDamageRatio = cfg.splashDamageRatio,
                        bulletKind = "melon"
                    )
                )
                e.add(
                    Renderable(
                        shape = Renderable.Shape.RECT,
                        color = Renderable.PLANT_WINTERMELON,
                        width = Grid.CELL_W * 0.7f,
                        height = Grid.CELL_H * 0.78f,
                        zOrder = 10 + row
                    )
                )
            }
            else -> error("Unknown plant type: ${cfg.type}")
        }

        // 夜间植物：若配置 nightOnly，附加 Sleep 组件（若上面 when 已加则无副作用）
        if (cfg.nightOnly && e.get<Sleep>() == null) {
            e.add(Sleep(asleep = false))
        }

        // 附加雪碧图组件（缺失资源时渲染系统会自动降级为色块）
        if (cfg.spriteKey.isNotEmpty()) {
            e.add(SpriteRenderable(spriteKey = cfg.spriteKey, animation = "idle", zOrder = 10 + row))
        }

        // M9: 附加单张静态 PNG 渲染组件。
        //   - 约定 key = "sprites/plants/<type>"；资源缺失时 StaticSpriteRenderSystem 静默跳过
        //   - 色块 Renderable 保留作为兜底，不删
        //   - 静态图尺寸比色块略大：AI 生成的角色有一些空白余量，稍放大贴近色块视觉
        staticSpriteFor(cfg.type)?.let { (w, h) ->
            e.add(
                StaticSpriteRenderable(
                    spriteKey = "sprites/plants/${cfg.type}",
                    widthPx = w,
                    heightPx = h,
                    zOrder = 11 + row // 比色块 (10+row) 稍大，确保盖在上面
                )
            )
        }
        return e
    }

    /**
     * 每种植物的静态图绘制尺寸（虚拟像素）。未列出的类型不挂静态图组件。
     * 取值思路：以 [Grid.CELL_W]×[Grid.CELL_H] 为基准略放大，让角色视觉饱满占满一格。
     */
    private fun staticSpriteFor(type: String): Pair<Float, Float>? = when (type) {
        "sunflower", "wallnut", "pumpkin", "cherrybomb" ->
            Grid.CELL_W * 0.95f to Grid.CELL_H * 1.05f
        "peashooter", "repeater", "snowpea", "torchwood", "wintermelon" ->
            Grid.CELL_W * 0.95f to Grid.CELL_H * 1.10f
        "tallnut" ->
            Grid.CELL_W * 0.95f to Grid.CELL_H * 1.25f
        // puffshroom 暂无美术图
        else -> null
    }
}
