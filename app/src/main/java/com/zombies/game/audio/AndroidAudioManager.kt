package com.zombies.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Android 平台的短音效播放器（SoundPool 实现）。
 *
 * - 在构造时尝试从 res/raw 加载每个 SoundId 对应的资源。
 * - 若资源不存在（resId == 0），静默跳过；播放时若未成功加载则降级为 no-op。
 * - 静音开关通过 [setMuted] 运行时切换；静音态下 play 直接返回。
 *
 * 资源目录：res/raw/sfx_plant.ogg 等；M7 阶段未必提供全部资源，系统会自适应。
 */
class AndroidAudioManager(context: Context, initialMuted: Boolean = false) : AudioManager {

    private val appContext = context.applicationContext
    private val soundPool: SoundPool
    private val soundIdToSample: MutableMap<SoundId, Int> = HashMap()

    @Volatile private var muted: Boolean = initialMuted

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(attrs)
            .build()

        val resources = appContext.resources
        val pkg = appContext.packageName
        for (id in SoundId.values()) {
            val resId = resources.getIdentifier(id.resName, "raw", pkg)
            if (resId != 0) {
                runCatching {
                    val sampleId = soundPool.load(appContext, resId, 1)
                    soundIdToSample[id] = sampleId
                }
            }
        }
    }

    override fun play(id: SoundId) {
        if (muted) return
        val sample = soundIdToSample[id] ?: return
        runCatching {
            soundPool.play(sample, VOLUME, VOLUME, 1, 0, 1f)
        }
    }

    override fun setMuted(muted: Boolean) {
        this.muted = muted
    }

    override fun isMuted(): Boolean = muted

    override fun release() {
        runCatching { soundPool.release() }
        soundIdToSample.clear()
    }

    companion object {
        private const val MAX_STREAMS = 8
        private const val VOLUME = 1.0f
    }
}
