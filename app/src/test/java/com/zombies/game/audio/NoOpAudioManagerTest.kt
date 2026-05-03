package com.zombies.game.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoOpAudioManagerTest {

    @Test
    fun `play does not throw`() {
        val am = NoOpAudioManager()
        for (id in SoundId.values()) {
            am.play(id)
        }
    }

    @Test
    fun `mute toggle persists within instance`() {
        val am = NoOpAudioManager()
        assertFalse(am.isMuted())
        am.setMuted(true)
        assertTrue(am.isMuted())
        am.setMuted(false)
        assertFalse(am.isMuted())
    }

    @Test
    fun `release is no-op safe`() {
        val am = NoOpAudioManager()
        am.release()
        // 调用第二次也安全
        am.release()
    }
}
