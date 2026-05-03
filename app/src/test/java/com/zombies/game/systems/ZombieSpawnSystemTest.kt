package com.zombies.game.systems

import com.zombies.game.components.ZombieTag
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.SpawnEntry
import com.zombies.game.config.WaveConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class ZombieSpawnSystemTest {

    private fun newCtx(entries: List<SpawnEntry>): GameContext = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type },
        wave = WaveConfig(
            levelId = "t",
            totalDurationMs = 60_000L,
            entries = entries
        )
    )

    private fun countZombies(world: World): Int {
        var n = 0
        world.forEachWith<ZombieTag> { _, _ -> n++ }
        return n
    }

    @Test
    fun spawnAtScheduledTimes() {
        val ctx = newCtx(
            listOf(
                SpawnEntry(atMs = 1_000L, zombieType = "basic", row = 0),
                SpawnEntry(atMs = 2_500L, zombieType = "basic", row = 2)
            )
        )
        val world = World()
        val sys = ZombieSpawnSystem(ctx, random = Random(1))
        world.addSystem(sys)

        // t = 500ms，无生成
        world.update(500L)
        assertEquals(0, countZombies(world))

        // t = 1100ms，首条触发（500+600=1100 >= 1000）
        world.update(600L)
        assertEquals(1, countZombies(world))
        assertEquals(1, ctx.nextSpawnIndex)

        // t = 2500ms，第二条触发
        world.update(1400L)
        assertEquals(2, countZombies(world))
        assertEquals(2, ctx.nextSpawnIndex)
    }

    @Test
    fun randomRowWhenNegative() {
        val ctx = newCtx(listOf(SpawnEntry(atMs = 0L, zombieType = "basic", row = -1)))
        val world = World()
        world.addSystem(ZombieSpawnSystem(ctx, random = Random(0)))
        world.update(100L)
        assertEquals(1, countZombies(world))
    }

    @Test
    fun stopsWhenNotRunning() {
        val ctx = newCtx(listOf(SpawnEntry(atMs = 0L, zombieType = "basic", row = 0)))
        ctx.state = GameContext.State.DEFEAT
        val world = World()
        world.addSystem(ZombieSpawnSystem(ctx))
        world.update(1_000L)
        assertEquals(0, countZombies(world))
        assertEquals(0, ctx.nextSpawnIndex)
    }
}
