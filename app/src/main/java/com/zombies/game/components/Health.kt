package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 生命值组件。hp <= 0 时由对应系统标记实体死亡。
 */
data class Health(
    var hp: Int,
    val maxHp: Int = hp
) : Component {
    val isDead: Boolean get() = hp <= 0
    fun damage(amount: Int) { hp = (hp - amount).coerceAtLeast(0) }
}
