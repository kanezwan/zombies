package com.zombies.game.components

import com.zombies.game.ecs.Component

/** 植物标签 + 类型键（用于识别种类） */
data class PlantTag(val type: String) : Component

/** 僵尸标签（M3 暂未接入，M4 启用） */
data class ZombieTag(val type: String) : Component

/** 子弹标签 */
data class ProjectileTag(val kind: String = "pea") : Component

/** 阳光标签 */
class SunTag : Component {
    companion object INSTANCE : Component
}
