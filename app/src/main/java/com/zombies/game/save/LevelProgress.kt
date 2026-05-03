package com.zombies.game.save

/**
 * 单关卡进度：
 *  - unlocked：是否解锁（可被点击进入）
 *  - cleared：是否通关
 *  - bestTimeMs：最佳通关用时（未通关时为 null）
 *  - bestSunLeft：最佳阳光结余（未通关时为 null）
 */
data class LevelProgress(
    val levelId: String,
    val unlocked: Boolean = false,
    val cleared: Boolean = false,
    val bestTimeMs: Long? = null,
    val bestSunLeft: Int? = null
)

/**
 * 关卡通关统计，通关时由 SaveManager.onLevelCleared 传入。
 */
data class ClearStats(
    val timeMs: Long,
    val sunLeft: Int
)
