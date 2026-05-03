package com.zombies.game.systems

import com.zombies.game.components.Producer
import com.zombies.game.components.Sun
import com.zombies.game.components.Transform
import com.zombies.game.ecs.World
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProducerSystemTest {

    private fun countSuns(world: World): Int {
        var n = 0
        world.forEachWith<Sun> { _, _ -> n++ }
        return n
    }

    @Test
    fun startupDelayBlocksProduction() {
        val world = World()
        val sys = ProducerSystem()
        world.addSystem(sys)
        world.createEntity().apply {
            add(Transform(100f, 100f))
            add(Producer(intervalMs = 1000, amount = 25, startupDelayMs = 2000))
        }
        // tick 1s：仍在延迟中，不产
        world.update(1000)
        assertEquals(0, countSuns(world))
    }

    @Test
    fun producesAfterInterval() {
        val world = World()
        world.addSystem(ProducerSystem())
        world.createEntity().apply {
            add(Transform(100f, 100f))
            add(Producer(intervalMs = 500, amount = 25, startupDelayMs = 0))
        }
        world.update(200)
        assertEquals(0, countSuns(world))
        world.update(400) // 累计 600 >= 500 → 产出
        assertEquals(1, countSuns(world))
    }

    @Test
    fun multipleProducersAccumulateIndependently() {
        val world = World()
        world.addSystem(ProducerSystem())
        repeat(3) {
            world.createEntity().apply {
                add(Transform(100f + it * 50, 100f))
                add(Producer(intervalMs = 300, amount = 25, startupDelayMs = 0))
            }
        }
        world.update(300)
        assertEquals(3, countSuns(world))
    }

    @Test
    fun sunEntityCarriesCorrectValue() {
        val world = World()
        world.addSystem(ProducerSystem())
        world.createEntity().apply {
            add(Transform(200f, 200f))
            add(Producer(intervalMs = 100, amount = 40, startupDelayMs = 0))
        }
        world.update(100)
        var found = false
        world.forEachWith<Sun> { _, sun ->
            assertEquals(40, sun.value)
            found = true
        }
        assertTrue(found)
    }
}
