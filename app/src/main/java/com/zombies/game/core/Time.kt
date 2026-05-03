package com.zombies.game.core

/**
 * 简单倒计时定时器，配合 ECS 在 [System.update] 中使用。
 *
 * 用法：
 * ```
 * val timer = Timer(1500L)
 * fun update(dtMs: Long) {
 *     if (timer.tick(dtMs)) { fire() }
 * }
 * ```
 */
class Timer(var intervalMs: Long, startElapsedMs: Long = 0L) {

    var elapsedMs: Long = startElapsedMs
        private set

    /**
     * 推进时间，若达到/超过 [intervalMs] 则返回 true 并自动减去一个周期。
     * 一次 tick 最多触发一次（防止大 dt 导致重复触发）。
     */
    fun tick(dtMs: Long): Boolean {
        elapsedMs += dtMs
        if (elapsedMs >= intervalMs) {
            elapsedMs -= intervalMs
            return true
        }
        return false
    }

    fun reset(startElapsedMs: Long = 0L) {
        elapsedMs = startElapsedMs
    }

    fun progress(): Float =
        if (intervalMs <= 0) 1f else (elapsedMs.toFloat() / intervalMs).coerceIn(0f, 1f)
}
