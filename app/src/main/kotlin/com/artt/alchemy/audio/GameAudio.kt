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

    /** The player's effects volume from 0 to 1, applied to every effect started after it is set. */
    var level = 1f

    fun play(sound: Sound) {
        val volume = EFFECT_VOLUME * level
        pool.play(ids.getValue(sound), volume, volume, 1, 0, 1f)
    }

    fun release() = pool.release()

    private companion object {
        const val MAX_STREAMS = 4
        const val EFFECT_VOLUME = 0.8f
    }
}

/** The looping background track, created on first start and kept paused while the app is hidden. */
class BackgroundMusic(private val context: Context) {
    private var current: MediaPlayer? = null
    private var upcoming: MediaPlayer? = null
    private var fade: ValueAnimator? = null
    private var volume = 0f
    private var audible = false

    /** The player's music volume from 0 to 1; a playing track follows it at once. */
    var level = 1f
        set(value) {
            field = value
            if (!audible) return
            fade?.cancel()
            volume = MUSIC_VOLUME * value
            current?.setVolume(volume, volume)
            upcoming?.setVolume(volume, volume)
        }

    fun start() {
        val playing = current ?: newPlayer()?.also { first ->
            current = first
            chain(first)
        } ?: return
        if (!playing.isPlaying) playing.start()
        audible = true
        fadeTo(MUSIC_VOLUME * level) {}
    }

    fun pause() {
        audible = false
        val playing = current?.takeIf { it.isPlaying } ?: return
        fadeTo(0f) { playing.pause() }
    }

    fun release() {
        fade?.cancel()
        fade = null
        current?.release()
        upcoming?.release()
        current = null
        upcoming = null
    }

    // Looping a single MediaPlayer leaves an audible gap at the loop point. A second player, already
    // prepared, takes over the moment the first ends, so the track repeats without a break.
    private fun chain(player: MediaPlayer) {
        val next = newPlayer()
        upcoming = next
        next?.let(player::setNextMediaPlayer)
        player.setOnCompletionListener { finished ->
            finished.release()
            current = upcoming
            upcoming = null
            current?.let(::chain)
        }
    }

    private fun newPlayer(): MediaPlayer? = MediaPlayer.create(context, R.raw.music_background, GameAudioAttributes, 0)
        ?.apply { setVolume(volume, volume) }

    private fun fadeTo(target: Float, onEnd: () -> Unit) {
        fade?.cancel()
        fade = ValueAnimator.ofFloat(volume, target).apply {
            duration = FADE_MILLIS
            addUpdateListener { animator ->
                volume = animator.animatedValue as Float
                current?.setVolume(volume, volume)
                upcoming?.setVolume(volume, volume)
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
