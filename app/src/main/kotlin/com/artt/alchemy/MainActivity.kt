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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artt.alchemy.ui.AlchemyApp
import com.artt.alchemy.ui.AlchemyViewModel
import com.artt.alchemy.ui.StudioIntro
import com.artt.alchemy.ui.components.systemAnimationsOff

class MainActivity : ComponentActivity() {
    private var introComplete by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A recreated activity resumes the game rather than replaying the studio intro.
        introComplete = savedInstanceState != null
        // The scene background is always dark, so system bar icons stay light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val game: AlchemyViewModel = viewModel()
            if (introComplete) {
                AlchemyApp(game)
            } else {
                val reducedMotion = game.state.progress.reducedMotion ?: systemAnimationsOff(LocalContext.current)
                StudioIntro(reducedMotion = reducedMotion, onFinished = { introComplete = true })
            }
        }
    }

    override fun onStop() {
        // Leaving during the intro skips its remainder; returning never starts it again.
        introComplete = true
        super.onStop()
    }
}
