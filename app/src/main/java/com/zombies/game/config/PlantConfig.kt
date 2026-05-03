package com.zombies.game.config

/**
 * 植物属性（从 assets/config/plants.json 解析）。
 *
 * 数值含义见 doc/zombies.md 第一期属性表。
 */
data class PlantConfig(
    val type: String,
    val cost: Int,
    val cooldownMs: Long,
    val hp: Int,
    // 向日葵
    val produceIntervalMs: Long = 0,
    val produceAmount: Int = 0,
    // 射手类
    val attackIntervalMs: Long = 0,
    val damage: Int = 0,
    val projectileSpeed: Float = 0f,
    val shotsPerFire: Int = 1,
    val shotGapMs: Long = 120,
    // 减速（寒冰射手）
    val slowMultiplier: Float = 1f,
    val slowDurationMs: Long = 0,
    // 西瓜投手：命中+溅射（同行前方一定距离内的其他僵尸吃 ratio×damage，不叠加 slow）
    val splashRadius: Float = 0f,
    val splashDamageRatio: Float = 0f,
    // 樱桃炸弹
    val fuseMs: Long = 0,
    val explodeRadius: Float = 0f,
    val explodeDamage: Int = 0,
    // 高坚果 / 南瓜头：是否免疫撑杆跳（默认 false，即可被跳越）
    val polevaultImmune: Boolean = false,
    // 火炬树桩：子弹穿过后伤害倍率（默认 1，即不加成）
    val torchMultiplier: Float = 1f,
    /** 可选：雪碧图 basePath，如 "sprites/plants/peashooter"；为空表示仅色块渲染 */
    val spriteKey: String = "",
    /** 是否"夜间植物"（蘑菇类）：白天睡眠不射击，夜间正常工作 */
    val nightOnly: Boolean = false
) {
    companion object {
        /** 兜底默认值（当 JSON 不存在或解析失败时使用） */
        val SUNFLOWER = PlantConfig(
            type = "sunflower",
            cost = 50,
            cooldownMs = 7500L,
            hp = 300,
            produceIntervalMs = 24000L,
            produceAmount = 25
        )
        val PEASHOOTER = PlantConfig(
            type = "peashooter",
            cost = 100,
            cooldownMs = 7500L,
            hp = 300,
            attackIntervalMs = 1500L,
            damage = 20,
            projectileSpeed = 520f
        )
        val REPEATER = PlantConfig(
            type = "repeater",
            cost = 200,
            cooldownMs = 7500L,
            hp = 300,
            attackIntervalMs = 1500L,
            damage = 20,
            projectileSpeed = 520f,
            shotsPerFire = 2,
            shotGapMs = 140L
        )
        val SNOWPEA = PlantConfig(
            type = "snowpea",
            cost = 175,
            cooldownMs = 7500L,
            hp = 300,
            attackIntervalMs = 1500L,
            damage = 20,
            projectileSpeed = 520f,
            slowMultiplier = 0.5f,
            slowDurationMs = 3000L
        )
        val WALLNUT = PlantConfig(
            type = "wallnut",
            cost = 50,
            cooldownMs = 30_000L,
            hp = 4000
        )
        val CHERRY_BOMB = PlantConfig(
            type = "cherrybomb",
            cost = 150,
            cooldownMs = 50_000L,
            hp = 300,
            fuseMs = 1200L,
            explodeRadius = 260f, // ~ 1.4 格
            explodeDamage = 1800
        )
        val TALLNUT = PlantConfig(
            type = "tallnut",
            cost = 125,
            cooldownMs = 30_000L,
            hp = 8000,
            polevaultImmune = true
        )
        val PUMPKIN = PlantConfig(
            type = "pumpkin",
            cost = 125,
            cooldownMs = 30_000L,
            hp = 4000
        )
        val TORCHWOOD = PlantConfig(
            type = "torchwood",
            cost = 175,
            cooldownMs = 7500L,
            hp = 300,
            torchMultiplier = 2f
        )
        val PUFFSHROOM = PlantConfig(
            type = "puffshroom",
            cost = 0,
            cooldownMs = 7500L,
            hp = 300,
            attackIntervalMs = 1500L,
            damage = 20,
            projectileSpeed = 420f,
            nightOnly = true
        )
        val WINTERMELON = PlantConfig(
            type = "wintermelon",
            cost = 200,
            cooldownMs = 7500L,
            hp = 300,
            attackIntervalMs = 2000L,
            damage = 60,
            projectileSpeed = 420f,
            slowMultiplier = 0.5f,
            slowDurationMs = 2000L,
            splashRadius = 120f,
            splashDamageRatio = 0.3f
        )
        val DEFAULTS = listOf(
            SUNFLOWER, PEASHOOTER, REPEATER, SNOWPEA,
            WALLNUT, TALLNUT, PUMPKIN, CHERRY_BOMB, TORCHWOOD,
            PUFFSHROOM, WINTERMELON
        )
    }
}
