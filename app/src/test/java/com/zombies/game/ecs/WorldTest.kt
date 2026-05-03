package com.zombies.game.ecs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private data class Pos(var x: Float, var y: Float) : Component
private data class Vel(var vx: Float, var vy: Float) : Component
private data class Tag(val name: String) : Component

class WorldTest {

    @Test
    fun `add and get component`() {
        val w = World()
        val e = w.createEntity().add(Pos(1f, 2f)).add(Tag("a"))
        w.update(0L) // flush

        val got = w.getEntity(e.id)
        assertNotNull(got)
        assertEquals(Pos(1f, 2f), got!!.get<Pos>())
        assertEquals("a", got.get<Tag>()?.name)
        assertNull(got.get<Vel>())
    }

    @Test
    fun `remove component works`() {
        val w = World()
        val e = w.createEntity().add(Pos(0f, 0f)).add(Vel(1f, 1f))
        w.update(0L)
        assertTrue(e.has<Vel>())
        e.remove<Vel>()
        assertNull(e.get<Vel>())
    }

    @Test
    fun `system runs in priority order and sees delta`() {
        val w = World()
        val e = w.createEntity().add(Pos(0f, 0f)).add(Vel(10f, 0f))

        val log = mutableListOf<Int>()
        w.addSystem(object : GameSystem(priority = 10) {
            override fun update(world: World, dtMs: Long) { log.add(10) }
        })
        w.addSystem(object : GameSystem(priority = 1) {
            override fun update(world: World, dtMs: Long) {
                log.add(1)
                world.forEachWith2<Pos, Vel> { _, p, v ->
                    p.x += v.vx * dtMs / 1000f
                    p.y += v.vy * dtMs / 1000f
                }
            }
        })

        w.update(1000L)

        assertEquals(listOf(1, 10), log)
        assertEquals(10f, e.get<Pos>()!!.x, 1e-4f)
    }

    @Test
    fun `remove entity is deferred until flush`() {
        val w = World()
        val e1 = w.createEntity().add(Tag("a"))
        val e2 = w.createEntity().add(Tag("b"))
        w.update(0L)
        assertEquals(2, w.entityCount())

        w.removeEntity(e1.id)
        // 未调用 update 前仍存在（等待 flush）
        assertEquals(2, w.entityCount())

        w.update(0L)
        assertEquals(1, w.entityCount())
        assertEquals("b", w.getEntity(e2.id)!!.get<Tag>()!!.name)
    }

    @Test
    fun `hasAll filters correctly`() {
        val w = World()
        val e = w.createEntity().add(Pos(0f, 0f)).add(Vel(0f, 0f))
        assertTrue(e.hasAll(Pos::class, Vel::class))
        val e2 = w.createEntity().add(Pos(0f, 0f))
        assertTrue(!e2.hasAll(Pos::class, Vel::class))
    }
}
