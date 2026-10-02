package com.artt.alchemy

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
            ScaledUpOnLargeScreens {
                if (introComplete) {
                    AlchemyApp(sceneReady = sceneReady.value)
                } else {
                    StudioIntro(ready = sceneReady.value, reducedMotion = reducedMotion, onFinished = { introComplete = true })
                }
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

/**
 * A tablet shows the phone layout enlarged, as if its shorter side were [LAYOUT_MAX_SHORT_SIDE] wide, instead of
 * phone-sized controls and text lost on a wide screen. Workspace positions are fractions, so nothing moves.
 */
@Composable
private fun ScaledUpOnLargeScreens(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val shortSide = with(density) { minOf(windowSize.width, windowSize.height).toDp() }
    val scale = (shortSide / LAYOUT_MAX_SHORT_SIDE).coerceAtLeast(1f)
    CompositionLocalProvider(LocalDensity provides Density(density.density * scale, density.fontScale), content = content)
}

private val LAYOUT_MAX_SHORT_SIDE = 600.dp
