package com.zombies.game.entity

import com.zombies.game.components.Pickable
import com.zombies.game.components.Renderable
import com.zombies.game.components.StaticSpriteRenderable
import com.zombies.game.components.Sun
import com.zombies.game.components.SunTag
import com.zombies.game.components.Transform
import com.zombies.game.components.Velocity
import com.zombies.game.core.Grid
import com.zombies.game.ecs.Entity
import com.zombies.game.ecs.World

object SunFactory {

    private const val RADIUS = 42f
    private const val LIFETIME_MS = 8_000L
    private const val FALL_SPEED = 120f      // 虚拟像素/秒

    /** 天降阳光：从顶部随机 x 落下到草坪内随机 y */
    fun createSky(world: World, virtualWidth: Float): Entity {
        val x = (Grid.ORIGIN_X + 60f) +
            Math.random().toFloat() * (Grid.WIDTH - 120f)
        val targetY = Grid.ORIGIN_Y + 120f +
            Math.random().toFloat() * (Grid.HEIGHT - 240f)
        return buildSun(world, x, -RADIUS, targetY, FALL_SPEED, 25, "sky")
    }

    /** 向日葵吐阳光：在向日葵附近弹出，短距离落下 */
    fun createFromSunflower(world: World, srcX: Float, srcY: Float, amount: Int): Entity {
        val targetY = srcY + 30f
        return buildSun(world, srcX, srcY - 10f, targetY, FALL_SPEED, amount, "sunflower")
    }

    private fun buildSun(
        world: World,
        x: Float, y: Float, targetY: Float,
        vy: Float, value: Int, source: String
    ): Entity {
        val e = world.createEntity()
        e.add(Transform(x, y))
        e.add(Velocity(vx = 0f, vy = vy))
        e.add(Sun(value = value, targetY = targetY, lifetimeMs = LIFETIME_MS, source = source))
        e.add(Pickable(radius = RADIUS))
        e.add(SunTag)
        e.add(
            Renderable(
                shape = Renderable.Shape.CIRCLE,
                color = Renderable.SUN,
                width = RADIUS * 2,
                height = RADIUS * 2,
                zOrder = 100
            )
        )
        // M9: 附加单张静态 PNG（缺图时自动回落到色块）
        e.add(
            StaticSpriteRenderable(
                spriteKey = "sprites/effects/sun",
                widthPx = RADIUS * 2.2f,
                heightPx = RADIUS * 2.2f,
                zOrder = 101
            )
        )
        return e
    }
}
