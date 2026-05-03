package com.zombies.game.systems

import com.zombies.game.components.Health
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.ecs.World
import com.zombies.game.entity.ProjectileFactory
import com.zombies.game.entity.ZombieFactory
import com.zombies.game.level.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectileCollisionSystemTest {

    private fun newCtx() = GameContext(
        plants = PlantConfig.DEFAULTS.associateBy { it.type },
        zombies = ZombieConfig.DEFAULTS.associateBy { it.type }
    )

    @Test
    fun bulletHitsZombieAndDecreasesHp() {
        val world = World()
        world.addSystem(ProjectileCollisionSystem())
        val zombie = ZombieFactory.create(world, ZombieConfig.BASIC, row = 0)
        world.update(0L) // 刷新 pending
        val zx = zombie.get<com.zombies.game.components.Transform>()!!.x
        val bullet = ProjectileFactory.createPea(
            world = world, row = 0, startX = zx, startY = 0f, speed = 0f, damage = 40
        )
        world.update(0L)
        val hpBefore = zombie.get<Health>()!!.hp
        world.update(16L)
        val hpAfter = zombie.get<Health>()!!.hp
        assertEquals(hpBefore - 40, hpAfter)
        // 子弹应被销毁
        assertNull(world.getEntity(bullet.id))
    }

    @Test
    fun bulletMissesWrongRow() {
        val world = World()
        world.addSystem(ProjectileCollisionSystem())
        val zombie = ZombieFactory.create(world, ZombieConfig.BASIC, row = 0)
        world.update(0L)
        val zx = zombie.get<com.zombies.game.components.Transform>()!!.x
        val bullet = ProjectileFactory.createPea(
            world = world, row = 2, startX = zx, startY = 0f, speed = 0f, damage = 40
        )
        world.update(0L)
        world.update(16L)
        // 子弹还在，僵尸满血
        assertNotNull(world.getEntity(bullet.id))
        assertEquals(ZombieConfig.BASIC.hp, zombie.get<Health>()!!.hp)
    }

    @Test
    fun bulletMissesTooFar() {
        val world = World()
        world.addSystem(ProjectileCollisionSystem())
        val zombie = ZombieFactory.create(world, ZombieConfig.BASIC, row = 0)
        world.update(0L)
        val zx = zombie.get<com.zombies.game.components.Transform>()!!.x
        val bullet = ProjectileFactory.createPea(
            world = world, row = 0, startX = zx - 200f, startY = 0f, speed = 0f, damage = 40
        )
        world.update(0L)
        world.update(16L)
        assertNotNull(world.getEntity(bullet.id))
        assertEquals(ZombieConfig.BASIC.hp, zombie.get<Health>()!!.hp)
    }
}
