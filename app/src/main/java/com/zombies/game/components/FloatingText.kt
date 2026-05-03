package com.zombies.game.components

import android.graphics.Color
import com.zombies.game.ecs.Component

/**
 * 飘字组件（+50 积分、伤害数字、状态提示等通用视觉反馈）。
 *
 * 生命周期：
 *  - 每帧由 [com.zombies.game.systems.FloatingTextSystem] 把 [elapsedMs] 加 dtMs，
 *    达到 [totalMs] 后自动移除实体。
 *  - 由 [com.zombies.game.systems.FloatingTextRenderSystem] 以 [Transform] 为锚点绘制，
 *    y 随时间向上漂浮，alpha 随时间从 255 渐隐到 0。
 *
 * @param text     显示文字（如 "+50"）
 * @param color    文字基色（alpha 分量会被运行时覆盖）
 * @param textSize 字号（虚拟像素）
 * @param totalMs  完整生命周期时长，常用 1000ms
 * @param vyPerSec y 上浮速度（负值向上，默认 -80 px/s）
 * @param elapsedMs 已经存活的时间（由系统推进，外部不要手动改）
 */
data class FloatingText(
    val text: String,
    val color: Int = Color.rgb(255, 220, 80),
    val textSize: Float = 42f,
    val totalMs: Long = 1000L,
    val vyPerSec: Float = -80f,
    var elapsedMs: Long = 0L
) : Component
