package com.zombies.game.level

import com.zombies.game.audio.AudioManager
import com.zombies.game.audio.NoOpAudioManager
import com.zombies.game.config.PlantConfig
import com.zombies.game.config.StageConfig
import com.zombies.game.config.WaveConfig
import com.zombies.game.config.ZombieConfig
import com.zombies.game.save.ClearStats

/**
 * 运行时上下文：承载跨系统共享的非 ECS 数据。
 *
 * 非 ECS 的原因：这些数据是"全局单例"（玩家阳光、冷却表、格子占用表、配置表、关卡进度），
 * 不适合挂在某个 Entity 上；由 [com.zombies.game.ecs.World] 的系统引用访问。
 */
class GameContext(
    val economy: Economy = Economy(initialSun = 50),
    val occupancy: GridOccupancy = GridOccupancy(),
    val plants: Map<String, PlantConfig>,
    val zombies: Map<String, ZombieConfig> = ZombieConfig.DEFAULTS.associateBy { it.type },
    val wave: WaveConfig = WaveConfig.LEVEL_1,
    /** 音效管理器；测试或资源缺失环境默认使用 [NoOpAudioManager] */
    val audio: AudioManager = NoOpAudioManager(),
    /** 场景配置：决定背景颜色、天空是否自动掉阳光、生产率倍率、蘑菇是否醒着等 */
    val stage: StageConfig = StageConfig.DAY
) {
    /** 玩家当前从卡牌槽选中的植物类型；null 表示未选中 */
    @Volatile
    var selectedPlantType: String? = null

    /** 铲子模式：点击草坪植物时直接拔除 */
    @Volatile
    var shovelMode: Boolean = false

    /** 关卡累计毫秒（由 ZombieSpawnSystem 累加，供 UI 显示进度） */
    var levelElapsedMs: Long = 0L

    /** 下一条待生成波次条目的下标 */
    var nextSpawnIndex: Int = 0

    /** 游戏结果 */
    @Volatile
    var state: State = State.RUNNING

    /** 本局累计击杀僵尸数（HealthSystem 递增，UI 线程读取并上报存档） */
    @Volatile
    var killsThisRun: Int = 0

    /** 本局累计种植次数（PlantingSystem 递增） */
    @Volatile
    var plantedThisRun: Int = 0

    /** 胜利回调（GameOverSystem 在切换到 VICTORY 的第一帧触发一次） */
    @Volatile
    var onVictory: ((ClearStats) -> Unit)? = null

    /** 失败回调（GameOverSystem 在切换到 DEFEAT 的第一帧触发一次） */
    @Volatile
    var onDefeat: (() -> Unit)? = null

    enum class State { RUNNING, VICTORY, DEFEAT }

    /**
     * 当前场景下**可用的**植物列表（卡牌槽显示顺序）。
     *
     * 规则：
     *  - 白天（stage.skySunEnabled == true）→ 过滤掉 nightOnly 植物（蘑菇类不给种，避免白天废卡）
     *  - 夜间 → 过滤掉需要阳光的经典植物？本期不做此限制，夜间依然可以种太阳花等，
     *    只是生产率走 stage.producerRateMultiplier。
     *
     * 调用方：CardHudRenderSystem / CardTouchSystem 使用该列表保持一致。
     */
    fun availablePlants(): List<PlantConfig> {
        val all = plants.values.toList()
        return if (stage.allowNightOnlyPlants) {
            all
        } else {
            all.filter { !it.nightOnly }
        }
    }

    fun reset(initialSun: Int = 50) {
        economy.reset(initialSun)
        occupancy.reset()
        selectedPlantType = null
        shovelMode = false
        levelElapsedMs = 0L
        nextSpawnIndex = 0
        state = State.RUNNING
        killsThisRun = 0
        plantedThisRun = 0
    }
}
