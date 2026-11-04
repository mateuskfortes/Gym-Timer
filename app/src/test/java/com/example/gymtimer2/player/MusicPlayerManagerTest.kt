package com.example.gymtimer2.player

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicPlayerManagerTest {
    @Test
    fun audioFocusDuckingReducesAndRestoresFadeVolume() {
        assertEquals(0.2f, musicVolumeForAudioFocus(1f, shouldDuck = true), 0.001f)
        assertEquals(0.12f, musicVolumeForAudioFocus(0.6f, shouldDuck = true), 0.001f)
        assertEquals(0.6f, musicVolumeForAudioFocus(0.6f, shouldDuck = false), 0.001f)
    }
}
