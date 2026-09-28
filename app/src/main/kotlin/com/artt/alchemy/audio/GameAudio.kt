package com.artt.alchemy.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.annotation.RawRes
import com.artt.alchemy.R

enum class Sound(@param:RawRes val res: Int) {
    PLACE(R.raw.sfx_place),
    COMBINE(R.raw.sfx_combine),
    DISCOVER(R.raw.sfx_discover),
    NO_MATCH(R.raw.sfx_no_match),
    REMOVE(R.raw.sfx_remove),
    CLICK(R.raw.sfx_click),
    PAGE(R.raw.sfx_page)
}

private val GameAudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_GAME)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

/** Short effects, loaded up front so they play without delay. */
class SoundEffects(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(GameAudioAttributes).build()
    private val ids = Sound.entries.associateWith { pool.load(context, it.res, 1) }

    fun play(sound: Sound) {
        pool.play(ids.getValue(sound), EFFECT_VOLUME, EFFECT_VOLUME, 1, 0, 1f)
    }

    fun release() = pool.release()

    private companion object {
        const val MAX_STREAMS = 4
        const val EFFECT_VOLUME = 0.8f
    }
}

/** The looping background track, created on first start and kept paused while the app is hidden. */
class BackgroundMusic(private val context: Context) {
    private var player: MediaPlayer? = null
    private var fade: ValueAnimator? = null
    private var currentVolume = 0f

    fun start() {
        val current = player ?: MediaPlayer.create(context, R.raw.music_background, GameAudioAttributes, 0)?.apply {
            isLooping = true
            setVolume(0f, 0f)
        }?.also { player = it } ?: return
        if (!current.isPlaying) current.start()
        fadeTo(current, MUSIC_VOLUME) {}
    }

    fun pause() {
        val current = player?.takeIf { it.isPlaying } ?: return
        fadeTo(current, 0f) { current.pause() }
    }

    fun release() {
        fade?.cancel()
        fade = null
        player?.release()
        player = null
    }

    private fun fadeTo(target: MediaPlayer, volume: Float, onEnd: () -> Unit) {
        fade?.cancel()
        val from = currentVolume
        fade = ValueAnimator.ofFloat(from, volume).apply {
            duration = FADE_MILLIS
            addUpdateListener { animator ->
                currentVolume = animator.animatedValue as Float
                target.setVolume(currentVolume, currentVolume)
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled) onEnd()
                }
            })
            start()
        }
    }

    private companion object {
        // Quiet enough to sit under the effects.
        const val MUSIC_VOLUME = 0.26f
        const val FADE_MILLIS = 300L
    }
}

enum class Haptic(val effectId: Int) {
    TICK(VibrationEffect.EFFECT_TICK),
    CLICK(VibrationEffect.EFFECT_CLICK),
    HEAVY(VibrationEffect.EFFECT_HEAVY_CLICK),
    DOUBLE(VibrationEffect.EFFECT_DOUBLE_CLICK)
}

/** The platform's predefined touch vibrations, which follow the system's touch feedback strength. */
class Haptics(context: Context) {
    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator

    fun perform(haptic: Haptic) {
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createPredefined(haptic.effectId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            vibrator.vibrate(effect)
        }
    }
}
