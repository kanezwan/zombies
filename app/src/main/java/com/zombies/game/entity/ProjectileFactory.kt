package com.zombies.game.entity

import com.zombies.game.components.Projectile
import com.zombies.game.components.ProjectileTag
import com.zombies.game.components.Renderable
import com.zombies.game.components.StaticSpriteRenderable
import com.zombies.game.components.Transform
import com.zombies.game.components.Velocity
import com.zombies.game.core.Grid
import com.zombies.game.ecs.Entity
import com.zombies.game.ecs.World

/**
 * 子弹工厂：产出普通豌豆 / 冰豆子 / 西瓜。
 *
 * - Pea：直线飞，无状态效果
 * - IcePea：直线飞 + 命中附加 slow（snowpea）
 * - Melon：直线飞（视觉简化）+ 命中附加 slow + 同行前方溅射（wintermelon）
 */
object ProjectileFactory {

    private const val RADIUS = 14f
    private const val MELON_RADIUS = 20f
    private const val CAR_RADIUS = 55f

    fun createPea(
        world: World,
        row: Int,
        startX: Float,
        startY: Float,
        speed: Float,
        damage: Int
    ): Entity = createInternal(
        world, row, startX, startY, speed, damage,
        color = Renderable.BULLET_PEA,
        kind = "pea",
        radius = RADIUS,
        slowMultiplier = 1f,
        slowDurationMs = 0L,
        slowSource = "",
        splashRadius = 0f,
        splashDamageRatio = 0f
    )

    fun createIcePea(
        world: World,
        row: Int,
        startX: Float,
        startY: Float,
        speed: Float,
        damage: Int,
        slowMultiplier: Float,
        slowDurationMs: Long,
        slowSource: String
    ): Entity = createInternal(
        world, row, startX, startY, speed, damage,
        color = Renderable.BULLET_ICE,
        kind = "icepea",
        radius = RADIUS,
        slowMultiplier = slowMultiplier,
        slowDurationMs = slowDurationMs,
        slowSource = slowSource,
        splashRadius = 0f,
        splashDamageRatio = 0f
    )

    fun createMelon(
        world: World,
        row: Int,
        startX: Float,
        startY: Float,
        speed: Float,
        damage: Int,
        slowMultiplier: Float,
        slowDurationMs: Long,
        splashRadius: Float,
        splashDamageRatio: Float
    ): Entity = createInternal(
        world, row, startX, startY, speed, damage,
        color = Renderable.BULLET_MELON,
        kind = "melon",
        radius = MELON_RADIUS,
        slowMultiplier = slowMultiplier,
        slowDurationMs = slowDurationMs,
        slowSource = "wintermelon",
        splashRadius = splashRadius,
        splashDamageRatio = splashDamageRatio
    )

    /**
     * Boss 扔车专用投射物。
     *
     * 与 pea/melon 的关键区别：
     *  - 速度为负值（向左飞），maxX 设置为 -200（飞出草坪左侧后销毁）
     *  - kind = "car"，由 [com.zombies.game.systems.BossCarSystem] 单独处理碰撞逻辑
     *  - 碾压植物 + 高额单体伤害（不是豌豆的同行僵尸命中规则）
     *
     * [speed] 应传负值（例如 -720f）。
     */
    fun createCar(
        world: World,
        row: Int,
        startX: Float,
        startY: Float,
        speed: Float,
        damage: Int
    ): Entity {
        val e = world.createEntity()
        e.add(Transform(startX, startY))
        e.add(Velocity(vx = speed, vy = 0f))
        e.add(
            Projectile(
                row = row,
                damage = damage,
                // maxX 对"向左飞的车"并不适用，这里填一个大值让 ProjectileSystem 不提前清；
                // 真正的左侧销毁由 BossCarSystem 在 x <= -200 时负责。
                maxX = Grid.ORIGIN_X + Grid.WIDTH + 1000f
            )
        )
        e.add(ProjectileTag("car"))
        e.add(
            Renderable(
                shape = Renderable.Shape.RECT,
                color = Renderable.BOSS_CAR,
                width = CAR_RADIUS * 2.2f,
                height = CAR_RADIUS * 1.4f,
                zOrder = 220
            )
        )
        return e
    }

    private fun createInternal(
        world: World,
        row: Int,
        startX: Float,
        startY: Float,
        speed: Float,
        damage: Int,
        color: Int,
        kind: String,
        radius: Float,
        slowMultiplier: Float,
        slowDurationMs: Long,
        slowSource: String,
        splashRadius: Float,
        splashDamageRatio: Float
    ): Entity {
        val e = world.createEntity()
        e.add(Transform(startX, startY))
        e.add(Velocity(vx = speed, vy = 0f))
        e.add(
            Projectile(
                row = row,
                damage = damage,
                maxX = Grid.ORIGIN_X + Grid.WIDTH + 80f,
                slowMultiplier = slowMultiplier,
                slowDurationMs = slowDurationMs,
                slowSource = slowSource,
                splashRadius = splashRadius,
                splashDamageRatio = splashDamageRatio
            )
        )
        e.add(ProjectileTag(kind))
        e.add(
            Renderable(
                shape = Renderable.Shape.CIRCLE,
                color = color,
                width = radius * 2,
                height = radius * 2,
                zOrder = 200
            )
        )

        // M9: 附加单张静态 PNG 渲染组件（缺图时自动回落到上面的圆形色块）。
        //   - pea / icepea / melon 三种子弹均有独立美术
        //   - 目标绘制尺寸 = 圆直径 * 1.3（角色图外围留了些空白，放大到差不多撑满原圆占位）
        val spriteKey = when (kind) {
            "pea" -> "sprites/projectiles/bullet_pea"
            "icepea" -> "sprites/projectiles/bullet_snowpea"
            "melon" -> "sprites/projectiles/bullet_melon"
            else -> null
        }
        if (spriteKey != null) {
            val side = radius * 2f * 1.3f
            e.add(
                StaticSpriteRenderable(
                    spriteKey = spriteKey,
                    widthPx = side,
                    heightPx = side,
                    zOrder = 201
                )
            )
        }
        return e
    }
}
