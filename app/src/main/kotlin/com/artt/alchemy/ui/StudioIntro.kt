package com.artt.alchemy.ui

import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

/**
 * A silent intro prepared behind the system splash, with no manual skip. [onShowing] comes once the logo stands still,
 * so heavy work there neither holds back the start of the video nor shows: the video plays on its own surface.
 */
@Composable
fun StudioIntro(ready: Boolean, reducedMotion: Boolean, onShowing: () -> Unit, onFinished: () -> Unit) {
    val showing = rememberUpdatedState(onShowing)
    val finish = rememberUpdatedState(onFinished)
    val canPlay = rememberUpdatedState(ready)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    var video by remember { mutableStateOf<VideoView?>(null) }
    var position by rememberSaveable { mutableIntStateOf(0) }
    // The clip's own fade stops short of black; the intro finishes it before the game appears.
    var fadingOut by remember { mutableStateOf(false) }
    val blackout = remember { Animatable(0f) }
    var videoShown by remember { mutableStateOf(true) }

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
            if (reducedMotion) finish.value() else fadingOut = true
        }
    }
    DisposableEffect(video) {
        val stop = video?.followPlayback(onLogoStill = { showing.value() }, onEnding = { fadingOut = true })
        onDispose { stop?.invoke() }
    }
    LaunchedEffect(fadingOut) {
        if (fadingOut) {
            blackout.animateTo(1f, tween(FADE_OUT_MILLIS, easing = FastOutLinearInEasing))
            // The video's surface would show through as the black lifts, so it goes first.
            videoShown = false
            finish.value()
        }
    }
    BackHandler { /* The brief studio intro plays through; Back does not skip it. */ }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            // The game waits underneath; it must not be touched through the intro.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .testTag("studio_intro")
    ) {
        if (reducedMotion) {
            LaunchedEffect(Unit) { showing.value() }
            Image(
                painter = painterResource(R.drawable.artt_intro_poster),
                contentDescription = stringResource(R.string.studio_intro_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().testTag("studio_intro_poster")
            )
        } else if (videoShown) {
            IntroVideo(
                startAt = { position },
                mayPlay = { canPlay.value && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) },
                blackout = { blackout.value },
                onCreated = { video = it },
                onEnded = { fadingOut = true },
                onError = { finish.value() }
            )
        }
    }
}

/**
 * Watches the playback position on the main looper: the video runs on its own clock, and asking for a frame every
 * vsync just to read it would keep Compose busy. Reports the still logo, then the moment the ending fade should begin.
 */
private fun VideoView.followPlayback(onLogoStill: () -> Unit, onEnding: () -> Unit): () -> Unit {
    val check = object : Runnable {
        override fun run() {
            if (isPlaying && duration > 0) {
                if (currentPosition >= LOGO_STILL_MILLIS) onLogoStill()
                if (currentPosition >= duration - FADE_OUT_LEAD_MILLIS) return onEnding()
            }
            postDelayed(this, PLAYBACK_CHECK_MILLIS)
        }
    }
    post(check)
    return { removeCallbacks(check) }
}

@Composable
private fun IntroVideo(startAt: () -> Int, mayPlay: () -> Boolean, blackout: () -> Float, onCreated: (VideoView) -> Unit, onEnded: () -> Unit, onError: () -> Unit) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                contentDescription = context.getString(R.string.studio_intro_description)
                setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                setOnPreparedListener { player ->
                    player.setVolume(0f, 0f)
                    if (startAt() > 0) player.seekTo(startAt().toLong(), MediaPlayer.SEEK_CLOSEST)
                    if (mayPlay()) start()
                }
                setOnCompletionListener { onEnded() }
                setOnErrorListener { _, _, _ ->
                    onError()
                    true
                }
                setVideoURI(Uri.parse("android.resource://${context.packageName}/${R.raw.artt_intro}"))
                onCreated(this)
            }
        },
        onRelease = { it.stopPlayback() },
        // The video draws on its own surface below the window, so the black is painted over it.
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                drawRect(Color.Black, alpha = blackout())
            }
            .testTag("studio_intro_video")
    )
}

private const val LOGO_STILL_MILLIS = 700
private const val FADE_OUT_LEAD_MILLIS = 300
private const val PLAYBACK_CHECK_MILLIS = 32L
private const val FADE_OUT_MILLIS = 500
