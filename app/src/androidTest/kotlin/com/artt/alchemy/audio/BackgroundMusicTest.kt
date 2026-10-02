package com.artt.alchemy.audio

import android.content.BroadcastReceiver
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.SystemClock
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.MainActivity
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.game.WorkspaceState
import com.artt.alchemy.ui.waitForScene
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class BackgroundMusicTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(object : TestWatcher() {
        override fun starting(description: Description) {
            val store = ProgressStore(InstrumentationRegistry.getInstrumentation().targetContext)
            // A workspace left by another test would lie on this one's board.
            store.saveWorkspace(WorkspaceState())
            store.save(store.load().copy(onboardingSeen = true, reducedMotion = true, musicEnabled = false))
        }
    }).around(composeRule)

    @Test
    fun pauseAtLoopBoundaryStopsTheSuccessorWithoutUsingReleasedPlayer() = withPlayingMusic { music ->
        val first = composeRule.runOnUiThread { current(music)!! }
        val sought = CountDownLatch(1)
        composeRule.runOnUiThread {
            first.setOnSeekCompleteListener { sought.countDown() }
            first.seekTo(first.duration - 250)
        }
        check(sought.await(3, TimeUnit.SECONDS)) { "Music seek did not finish" }
        composeRule.runOnUiThread { music.pause() }
        composeRule.waitUntil(timeoutMillis = 3000) {
            composeRule.runOnUiThread { current(music) !== first && current(music)?.isPlaying == false }
        }
        // Let the delayed fade callback run before resuming or releasing the controller.
        SystemClock.sleep(800)
        composeRule.runOnUiThread {
            assertNotSame(first, current(music))
            assertFalse(current(music)!!.isPlaying)
            music.start()
        }
        waitForPlayback(music)
    }

    @Test
    fun changingLevelDuringFadeOutStillPausesPlayback() = withPlayingMusic { music ->
        composeRule.runOnUiThread {
            music.pause()
            music.level = 0.4f
        }
        composeRule.waitUntil(timeoutMillis = 3000) { composeRule.runOnUiThread { current(music)?.isPlaying == false } }
        composeRule.runOnUiThread { assertFalse(current(music)!!.isPlaying) }
    }

    @Test
    fun disconnectingHeadphonesPausesMusic() = withPlayingMusic { music ->
        composeRule.runOnUiThread {
            val context = composeRule.activity.applicationContext
            // This is a protected system broadcast: apps and adb shell cannot send it.
            val field = BackgroundMusic::class.java.declaredFields.firstOrNull { BroadcastReceiver::class.java.isAssignableFrom(it.type) }
            assertNotNull("Music must handle headphone disconnection", field)
            field!!.isAccessible = true
            (field.get(music) as BroadcastReceiver).onReceive(context, Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        }
        composeRule.waitUntil(timeoutMillis = 3000) { composeRule.runOnUiThread { current(music)?.isPlaying == false } }
        composeRule.runOnUiThread { assertFalse(current(music)!!.isPlaying) }
    }

    @Test
    fun transientInterruptionKeepsHeadphoneMonitoringUntilPlaybackResumes() = withPlayingMusic { music ->
        composeRule.runOnUiThread {
            val focusChange = BackgroundMusic::class.java.getDeclaredMethod("onFocusChange", Int::class.javaPrimitiveType).apply { isAccessible = true }
            focusChange.invoke(music, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            assertTrue("Headphone disconnect must still be monitored during a passing interruption", monitorsHeadphones(music))
            val receiver = BackgroundMusic::class.java.declaredFields.first { BroadcastReceiver::class.java.isAssignableFrom(it.type) }.apply { isAccessible = true }
            (receiver.get(music) as BroadcastReceiver).onReceive(composeRule.activity.applicationContext, Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            focusChange.invoke(music, AudioManager.AUDIOFOCUS_GAIN)
        }
        SystemClock.sleep(800)
        composeRule.runOnUiThread { assertFalse(current(music)!!.isPlaying) }
    }

    @Test
    fun headphoneMonitoringStartsBeforeTheDelayedIntro() {
        composeRule.waitForScene()
        composeRule.runOnUiThread {
            val music = BackgroundMusic(composeRule.activity.applicationContext)
            try {
                music.start()
                assertTrue("Headphone disconnect must be monitored before the first note", monitorsHeadphones(music))
            } finally {
                music.release()
            }
        }
    }

    private fun monitorsHeadphones(music: BackgroundMusic): Boolean = BackgroundMusic::class.java.getDeclaredField("noisyReceiverRegistered").let {
        it.isAccessible = true
        it.getBoolean(music)
    }

    private fun withPlayingMusic(test: (BackgroundMusic) -> Unit) {
        composeRule.waitForScene()
        val music = composeRule.runOnUiThread { BackgroundMusic(composeRule.activity.applicationContext).also { it.start() } }
        try {
            waitForPlayback(music)
            composeRule.runOnUiThread { music.level = 0.5f }
            test(music)
        } finally {
            composeRule.runOnUiThread { music.release() }
        }
    }

    private fun waitForPlayback(music: BackgroundMusic) {
        composeRule.waitUntil(timeoutMillis = 5000) { composeRule.runOnUiThread { current(music)?.isPlaying == true } }
    }

    // Accelerate the real 66.5-second track and inspect playback without adding test hooks to production.
    private fun current(music: BackgroundMusic): MediaPlayer? = BackgroundMusic::class.java.getDeclaredField("current").let {
        it.isAccessible = true
        it.get(music) as MediaPlayer?
    }
}
