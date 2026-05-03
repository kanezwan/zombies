package com.zombies.game.systems

import com.zombies.game.components.SpriteRenderable
import com.zombies.game.components.Transform
import com.zombies.game.ecs.World
import org.junit.Assert.assertEquals
import org.junit.Test

class AnimationSystemTest {

    @Test
    fun playedMsAccumulatesEachFrame() {
        val world = World()
        world.addSystem(AnimationSystem())
        val e = world.createEntity()
        e.add(Transform(0f, 0f))
        e.add(SpriteRenderable(spriteKey = "ignored", animation = "idle"))
        world.update(0L)

        world.update(16L)
        world.update(17L)
        world.update(20L)

        assertEquals(53L, e.get<SpriteRenderable>()!!.playedMs)
    }

    @Test
    fun multipleSpritesAdvanceIndependently() {
        val world = World()
        world.addSystem(AnimationSystem())
        val a = world.createEntity()
        val b = world.createEntity()
        a.add(SpriteRenderable(spriteKey = "a"))
        b.add(SpriteRenderable(spriteKey = "b"))
        b.get<SpriteRenderable>()!!.playedMs = 500L
        world.update(0L)

        world.update(100L)

        assertEquals(100L, a.get<SpriteRenderable>()!!.playedMs)
        assertEquals(600L, b.get<SpriteRenderable>()!!.playedMs)
    }
}
