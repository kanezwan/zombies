package com.zombies.game.ecs

import kotlin.reflect.KClass

/**
 * ECS 实体：一个唯一 id + 一组 [Component] 的集合。
 *
 * 性能考虑：
 *  - 用 HashMap<KClass, Component> 存储，O(1) 查找
 *  - 每个实体的组件数通常 <10，开销可忽略
 */
class Entity internal constructor(val id: Long) {

    private val components = HashMap<KClass<out Component>, Component>(8)

    /** 添加（或替换）组件 */
    fun add(component: Component): Entity {
        components[component::class] = component
        return this
    }

    /** 移除组件 */
    inline fun <reified T : Component> remove(): T? {
        @Suppress("UNCHECKED_CAST")
        return removeByClass(T::class) as T?
    }

    @PublishedApi
    internal fun removeByClass(clazz: KClass<out Component>): Component? = components.remove(clazz)

    /** 获取组件，不存在返回 null */
    inline fun <reified T : Component> get(): T? {
        @Suppress("UNCHECKED_CAST")
        return getByClass(T::class) as T?
    }

    @PublishedApi
    internal fun getByClass(clazz: KClass<out Component>): Component? = components[clazz]

    /** 获取组件，不存在抛异常 */
    inline fun <reified T : Component> require(): T =
        get<T>() ?: error("Entity $id missing component ${T::class.simpleName}")

    /** 是否包含某组件 */
    inline fun <reified T : Component> has(): Boolean = hasByClass(T::class)

    @PublishedApi
    internal fun hasByClass(clazz: KClass<out Component>): Boolean = components.containsKey(clazz)

    /** 是否包含全部指定类型的组件（用于 System 过滤） */
    fun hasAll(vararg classes: KClass<out Component>): Boolean {
        for (c in classes) if (!components.containsKey(c)) return false
        return true
    }

    override fun toString(): String =
        "Entity(id=$id, components=${components.keys.map { it.simpleName }})"
}
