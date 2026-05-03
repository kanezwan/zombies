package com.zombies.game.config

/**
 * 僵尸配置（数据驱动）。单位统一使用虚拟像素 & 毫秒。
 *
 * @property type 类型键，如 "basic" / "conehead" / "buckethead" / "flag" / "polevault" /
 *                "newspaper" / "football" / "jester"
 * @property hp 初始血量
 * @property speed 行走速度（虚拟像素 / 秒）
 * @property attackDamage 单次啃食伤害
 * @property attackIntervalMs 啃食间隔
 * @property jumpDistance 撑杆跳触发距离（仅 polevault 使用；0 表示不跳）
 * @property armorHp 外挂护甲血量（如 Newspaper 的报纸）；为 0 表示无护甲
 * @property enragedSpeedMultiplier 护甲破碎后的速度倍率（Newspaper 暴走）；1f 表示无变化
 * @property reflectProjectiles 是否反弹普通豌豆（Jester：被打到时子弹反向飞）
 * @property isBoss 是否为 Boss 类型（挂载 [com.zombies.game.components.Boss] 组件 + 免疫 slow/freeze + 独立 HUD）
 */
data class ZombieConfig(
    val type: String,
    val hp: Int,
    val speed: Float,
    val attackDamage: Int,
    val attackIntervalMs: Long,
    val jumpDistance: Float = 0f,
    val armorHp: Int = 0,
    val enragedSpeedMultiplier: Float = 1f,
    val reflectProjectiles: Boolean = false,
    val isBoss: Boolean = false
) {
    companion object {
        val BASIC = ZombieConfig(
            type = "basic",
            hp = 200,
            speed = 22f,
            attackDamage = 20,
            attackIntervalMs = 1_000L
        )
        val CONEHEAD = ZombieConfig(
            type = "conehead",
            hp = 570,
            speed = 22f,
            attackDamage = 20,
            attackIntervalMs = 1_000L
        )
        val BUCKETHEAD = ZombieConfig(
            type = "buckethead",
            hp = 1300,
            speed = 22f,
            attackDamage = 20,
            attackIntervalMs = 1_000L
        )
        val FLAG = ZombieConfig(
            type = "flag",
            hp = 200,
            speed = 28f, // 略快（带头冲锋示意）
            attackDamage = 20,
            attackIntervalMs = 1_000L
        )
        val POLEVAULT = ZombieConfig(
            type = "polevault",
            hp = 340,
            speed = 46f, // 快（跑动前）
            attackDamage = 20,
            attackIntervalMs = 1_000L,
            jumpDistance = 180f
        )
        val NEWSPAPER = ZombieConfig(
            type = "newspaper",
            hp = 200,
            speed = 18f,                       // 看报时较慢
            attackDamage = 20,
            attackIntervalMs = 1_000L,
            armorHp = 300,                     // 报纸护甲
            enragedSpeedMultiplier = 2.5f      // 报纸破碎后暴走
        )
        val FOOTBALL = ZombieConfig(
            type = "football",
            hp = 1600,
            speed = 45f,
            attackDamage = 20,
            attackIntervalMs = 1_000L
        )
        val JESTER = ZombieConfig(
            type = "jester",
            hp = 500,
            speed = 32f,
            attackDamage = 20,
            attackIntervalMs = 1_000L,
            reflectProjectiles = true
        )
        val BOSS_DRZOMBOSS = ZombieConfig(
            type = "bossdrzomboss",
            hp = 8000,
            speed = 12f,                       // 缓慢推进，靠技能压迫
            attackDamage = 60,                 // 单口硬肉攻速正常，但高伤
            attackIntervalMs = 1_200L,
            isBoss = true
        )

        val DEFAULTS: List<ZombieConfig> = listOf(
            BASIC, CONEHEAD, BUCKETHEAD, FLAG, POLEVAULT,
            NEWSPAPER, FOOTBALL, JESTER, BOSS_DRZOMBOSS
        )
    }
}
