package com.zombies.game.ecs

import android.graphics.Canvas
import java.util.concurrent.atomic.AtomicLong

/**
 * ECS 世界：实体容器 + 系统调度器。
 *
 * 设计：
 *  - 单线程（GameLoop 线程）访问，无需加锁
 *  - 实体增删通过缓冲队列延迟到帧末，避免迭代中修改集合
 *  - System 按 priority 升序执行
 */
class World {

    private val idGen = AtomicLong(1)
    private val entities = LinkedHashMap<Long, Entity>(64)

    private val systems = ArrayList<GameSystem>(16)
    private val renderSystems = ArrayList<RenderSystem>(8)

    private val pendingAdd = ArrayList<Entity>(16)
    private val pendingRemove = ArrayList<Long>(16)

    /** 累计游戏时间（毫秒），从 World 创建起累加 */
    var elapsedMs: Long = 0L
        private set

    // ---------------- Entity ----------------

    fun createEntity(): Entity {
        val e = Entity(idGen.getAndIncrement())
        pendingAdd.add(e)
        return e
    }

    fun removeEntity(id: Long) {
        pendingRemove.add(id)
    }

    fun getEntity(id: Long): Entity? = entities[id]

    /** 遍历所有实体（迭代时禁止修改集合，使用 [createEntity]/[removeEntity] 延迟生效） */
    inline fun forEach(action: (Entity) -> Unit) {
        for (e in snapshotEntities()) action(e)
    }

    @PublishedApi
    internal fun snapshotEntities(): Collection<Entity> = entities.values

    /** 按组件过滤遍历 */
    inline fun <reified A : Component> forEachWith(action: (Entity, A) -> Unit) {
        for (e in snapshotEntities()) {
            val a = e.get<A>() ?: continue
            action(e, a)
        }
    }

    inline fun <reified A : Component, reified B : Component> forEachWith2(
        action: (Entity, A, B) -> Unit
    ) {
        for (e in snapshotEntities()) {
            val a = e.get<A>() ?: continue
            val b = e.get<B>() ?: continue
            action(e, a, b)
        }
    }

    // ---------------- System ----------------

    fun addSystem(system: GameSystem) {
        systems.add(system)
        systems.sortBy { it.priority }
    }

    fun addRenderSystem(system: RenderSystem) {
        renderSystems.add(system)
        renderSystems.sortBy { it.priority }
    }

    // ---------------- Tick ----------------

    /** 由 GameLoop 每个固定步长调用 */
    fun update(dtMs: Long) {
        flushPending()
        elapsedMs += dtMs
        for (i in systems.indices) {
            systems[i].update(this, dtMs)
        }
        flushPending()
    }

    /** 由 GameLoop 每帧调用 */
    fun render(canvas: Canvas) {
        for (i in renderSystems.indices) {
            renderSystems[i].render(this, canvas)
        }
    }

    private fun flushPending() {
        if (pendingAdd.isNotEmpty()) {
            for (e in pendingAdd) entities[e.id] = e
            pendingAdd.clear()
        }
        if (pendingRemove.isNotEmpty()) {
            for (id in pendingRemove) entities.remove(id)
            pendingRemove.clear()
        }
    }

    fun clear() {
        entities.clear()
        pendingAdd.clear()
        pendingRemove.clear()
        systems.clear()
        renderSystems.clear()
        elapsedMs = 0L
    }

    fun entityCount(): Int = entities.size
}
