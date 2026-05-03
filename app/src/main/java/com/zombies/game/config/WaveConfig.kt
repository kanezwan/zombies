package com.zombies.game.config

/**
 * 单次生成条目：在 [atMs] 时（关卡开始后的绝对时间）在 [row] 行生成一只 [zombieType]。
 * [row] = -1 表示随机行。
 */
data class SpawnEntry(
    val atMs: Long,
    val zombieType: String,
    val row: Int = -1
)

/**
 * 关卡波次配置。
 * [totalDurationMs] 可用于显示进度条；超过该时间且无僵尸存活视为胜利（由 GameOverSystem 判断）。
 */
data class WaveConfig(
    val levelId: String,
    val totalDurationMs: Long,
    val entries: List<SpawnEntry>
) {
    companion object {
        /** 默认关卡 1：20 只僵尸，分散在 90 秒内 */
        val LEVEL_1: WaveConfig = run {
            val rows = 5
            val list = ArrayList<SpawnEntry>(20)
            var t = 15_000L
            for (i in 0 until 20) {
                list.add(SpawnEntry(atMs = t, zombieType = "basic", row = i % rows))
                t += 4_000L + (i % 3) * 500L
            }
            WaveConfig(levelId = "level_1", totalDurationMs = t + 5_000L, entries = list)
        }
    }
}
