package com.zombies.game.audio

/**
 * 音效播放接口：
 *  - play(id)：触发一次短音效
 *  - setMuted / isMuted：静音开关（会被持久化到 SaveManager）
 *
 * 具体实现见 [AndroidAudioManager]（SoundPool）与 [NoOpAudioManager]（测试/资源缺失时降级）。
 */
interface AudioManager {
    fun play(id: SoundId)
    fun setMuted(muted: Boolean)
    fun isMuted(): Boolean
    fun release()
}

/** 无声实现：测试与无资源场景使用 */
class NoOpAudioManager : AudioManager {
    private var muted: Boolean = false
    override fun play(id: SoundId) { /* no-op */ }
    override fun setMuted(muted: Boolean) { this.muted = muted }
    override fun isMuted(): Boolean = muted
    override fun release() { /* no-op */ }
}
