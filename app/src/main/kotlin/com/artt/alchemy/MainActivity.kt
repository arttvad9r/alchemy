package com.artt.alchemy

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.ui.AlchemyApp
import com.artt.alchemy.ui.StudioIntro
import com.artt.alchemy.ui.components.systemAnimationsOff

class MainActivity : ComponentActivity() {
    // The splash has gone and the scene is the player's; the music starts only then.
    private val sceneReady = mutableStateOf(false)
    private var introComplete by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        introComplete = savedInstanceState?.getBoolean(INTRO_COMPLETE_KEY) ?: false
        val reducedMotion = ProgressStore(this).load().reducedMotion ?: systemAnimationsOff(this)
        // The scene background is always dark, so system bar icons stay light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        leaveSystemStartingWindow()
        // A recreated activity shows no splash; a launch that skips it (from a test, say) must not wait forever.
        if (savedInstanceState != null) sceneReady.value = true
        window.decorView.postDelayed({ sceneReady.value = true }, SPLASH_FALLBACK_MILLIS)
        setContent {
            if (introComplete) {
                AlchemyApp(sceneReady = sceneReady.value)
            } else {
                StudioIntro(ready = sceneReady.value, reducedMotion = reducedMotion, onFinished = { introComplete = true })
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(INTRO_COMPLETE_KEY, introComplete)
        super.onSaveInstanceState(outState)
    }

    // The system starting window is plain black; remove it immediately into the studio intro.
    private fun leaveSystemStartingWindow() {
        splashScreen.setOnExitAnimationListener { splash ->
            splash.remove()
            sceneReady.value = true
        }
    }

    private companion object {
        const val INTRO_COMPLETE_KEY = "studio_intro_complete"
        const val SPLASH_FALLBACK_MILLIS = 1500L
    }
}
