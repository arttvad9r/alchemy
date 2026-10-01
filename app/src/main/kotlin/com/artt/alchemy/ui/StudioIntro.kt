package com.artt.alchemy.ui

import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.artt.alchemy.R
import kotlinx.coroutines.delay

/** A silent intro prepared behind the system splash, with no manual skip. */
@Composable
fun StudioIntro(ready: Boolean, reducedMotion: Boolean, onFinished: () -> Unit) {
    val finish = rememberUpdatedState(onFinished)
    val canPlay = rememberUpdatedState(ready)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    var video by remember { mutableStateOf<VideoView?>(null) }
    var position by rememberSaveable { mutableIntStateOf(0) }

    LifecycleResumeEffect(video, ready) {
        if (ready) video?.start()
        onPauseOrDispose {
            video?.let { view ->
                position = view.currentPosition
                view.pause()
            }
        }
    }
    // Pause the timer in the background. A failed decoder must not block the game forever.
    LaunchedEffect(ready, reducedMotion, lifecycleState) {
        if (ready && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
            delay(if (reducedMotion) 1000L else 4000L)
            finish.value()
        }
    }
    BackHandler { /* The brief studio intro plays through; Back does not skip it. */ }
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
                            if (position > 0) player.seekTo(position.toLong(), MediaPlayer.SEEK_CLOSEST)
                            if (canPlay.value && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
                        }
                        setOnCompletionListener { finish.value() }
                        setOnErrorListener { _, _, _ ->
                            finish.value()
                            true
                        }
                        setVideoURI(Uri.parse("android.resource://${context.packageName}/${R.raw.artt_intro}"))
                        video = this
                    }
                },
                onRelease = { it.stopPlayback() },
                modifier = Modifier.fillMaxSize().testTag("studio_intro_video")
            )
        }
    }
}
