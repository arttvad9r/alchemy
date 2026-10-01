package com.artt.alchemy.ui

import android.media.AudioManager
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.artt.alchemy.R
import com.artt.alchemy.ui.theme.AlchemyTheme
import kotlinx.coroutines.delay

/** A local, silent intro; completion, skip and playback failure all enter the game. */
@Composable
fun StudioIntro(reducedMotion: Boolean, onFinished: () -> Unit) {
    val finish = rememberUpdatedState(onFinished)
    // A stalled decoder must never prevent reaching the game. Reduced motion uses a still.
    LaunchedEffect(reducedMotion) {
        delay(if (reducedMotion) 1000L else 5000L)
        finish.value()
    }
    BackHandler { finish.value() }
    AlchemyTheme {
        Box(Modifier.fillMaxSize().background(Color.Black).testTag("studio_intro")) {
            if (reducedMotion) {
                Image(
                    painter = painterResource(R.drawable.artt_intro_poster),
                    contentDescription = stringResource(R.string.studio_intro_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().testTag("studio_intro_poster")
                )
            } else {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            contentDescription = context.getString(R.string.studio_intro_description)
                            setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                            setOnPreparedListener { player ->
                                player.setVolume(0f, 0f)
                                start()
                            }
                            setOnCompletionListener { finish.value() }
                            setOnErrorListener { _, _, _ ->
                                finish.value()
                                true
                            }
                            setVideoURI(Uri.parse("android.resource://${context.packageName}/${R.raw.artt_intro}"))
                        }
                    },
                    onRelease = { it.stopPlayback() },
                    modifier = Modifier.fillMaxSize().testTag("studio_intro_video")
                )
            }
            TextButton(
                onClick = { finish.value() },
                modifier = Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(16.dp).testTag("studio_intro_skip")
            ) {
                Text(stringResource(R.string.studio_intro_skip), color = Color.White, fontFamily = FontFamily.SansSerif)
            }
        }
    }
}
