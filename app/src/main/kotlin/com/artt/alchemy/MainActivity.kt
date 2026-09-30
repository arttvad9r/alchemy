package com.artt.alchemy

import android.graphics.Color
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.ui.AlchemyApp
import com.artt.alchemy.ui.components.systemAnimationsOff

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The scene background is always dark, so system bar icons stay light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        leaveSplashIntoTheScene()
        setContent { AlchemyApp() }
    }

    // The spell book swells and dissolves into the scene, which is already drawn behind it.
    private fun leaveSplashIntoTheScene() {
        val reducedMotion = ProgressStore(this).load().reducedMotion ?: systemAnimationsOff(this)
        splashScreen.setOnExitAnimationListener { splash ->
            if (reducedMotion) {
                splash.remove()
                return@setOnExitAnimationListener
            }
            splash.iconView?.animate()
                ?.scaleX(SPLASH_ICON_SCALE)
                ?.scaleY(SPLASH_ICON_SCALE)
                ?.setDuration(SPLASH_EXIT_MILLIS)
                ?.setInterpolator(AccelerateInterpolator())
                ?.start()
            splash.animate()
                .alpha(0f)
                .setDuration(SPLASH_EXIT_MILLIS)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { splash.remove() }
                .start()
        }
    }

    private companion object {
        const val SPLASH_EXIT_MILLIS = 420L
        const val SPLASH_ICON_SCALE = 1.35f
    }
}
