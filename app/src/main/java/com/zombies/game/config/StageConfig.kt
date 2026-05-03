package com.zombies.game.config

import android.graphics.Color

/**
 * 场景配置：决定场地视觉与"自然规则"。
 *
 * M9 引入两种：DAY（白天/草坪）与 NIGHT（夜间/草坪）。
 *
 * @property id 场景标识
 * @property backgroundColor 场地背景底色（渲染给 ShapeRenderSystem 绘制前清屏使用）
 * @property gridColorEven 偶数格颜色
 * @property gridColorOdd 奇数格颜色
 * @property skySunEnabled 天空是否自动掉落阳光；夜间通常关闭
 * @property producerRateMultiplier 生产型植物（太阳花）产率倍率
 * @property allowDayOnlyPlants 允许"仅白天"类植物正常工作（如豌豆射手、向日葵）
 * @property allowNightOnlyPlants 允许"仅夜间"类植物正常工作（如小喷菇等蘑菇类）
 */
data class StageConfig(
    val id: String,
    val backgroundColor: Int,
    val gridColorEven: Int,
    val gridColorOdd: Int,
    val skySunEnabled: Boolean,
    val producerRateMultiplier: Float = 1f,
    val allowDayOnlyPlants: Boolean = true,
    val allowNightOnlyPlants: Boolean = true
) {
    companion object {
        val DAY = StageConfig(
            id = "day",
            backgroundColor = Color.rgb(187, 222, 181),      // 淡草绿
            gridColorEven = Color.rgb(163, 206, 140),
            gridColorOdd = Color.rgb(140, 186, 120),
            skySunEnabled = true,
            producerRateMultiplier = 1f,
            allowDayOnlyPlants = true,
            allowNightOnlyPlants = false                     // 白天蘑菇睡觉
        )
        val NIGHT = StageConfig(
            id = "night",
            backgroundColor = Color.rgb(26, 35, 72),         // 深蓝紫夜空
            gridColorEven = Color.rgb(49, 59, 97),
            gridColorOdd = Color.rgb(38, 46, 82),
            skySunEnabled = false,                           // 夜间不自动掉阳光
            producerRateMultiplier = 1.2f,                   // 产率+20% 作为补偿
            allowDayOnlyPlants = true,
            allowNightOnlyPlants = true
        )
    }
}
