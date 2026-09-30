package com.artt.alchemy.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibrationEffect.Composition
import android.os.VibratorManager
import androidx.annotation.RawRes
import com.artt.alchemy.R
import kotlin.random.Random

/** A sound effect; [pitchSpread] is how far each play may drift from the recorded pitch, so repeats never sound canned. */
enum class Sound(@param:RawRes val res: Int, val pitchSpread: Float = 0f) {
    PLACE(R.raw.sfx_place, 0.06f),
    COMBINE(R.raw.sfx_combine, 0.02f),
    DISCOVER(R.raw.sfx_discover),
    DISCOVER_GRAND(R.raw.sfx_discover_grand),
    NO_MATCH(R.raw.sfx_no_match, 0.04f),
    REMOVE(R.raw.sfx_remove, 0.06f),
    CLICK(R.raw.sfx_click, 0.05f),
    PAGE(R.raw.sfx_page, 0.03f),
    PICKUP(R.raw.sfx_pickup, 0.08f),
    WHOOSH(R.raw.sfx_whoosh, 0.05f),
    HINT(R.raw.sfx_hint),
    ACHIEVEMENT(R.raw.sfx_achievement)
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

    /**
     * Plays [sound] leaning towards [pan] (-1 left to 1 right) and shifted by [semitones], with a small random drift
     * in pitch for the sounds that repeat often.
     */
    fun play(sound: Sound, pan: Float = 0f, semitones: Float = 0f) {
        val volume = EFFECT_VOLUME * level
        val (left, right) = stereoGains(pan)
        val drift = if (sound.pitchSpread > 0f) Random.nextFloat() * 2f * sound.pitchSpread - sound.pitchSpread else 0f
        val rate = (semitoneRate(semitones) * (1f + drift)).coerceIn(MIN_RATE, MAX_RATE)
        pool.play(ids.getValue(sound), volume * left, volume * right, 1, 0, rate)
    }

    fun release() = pool.release()

    private companion object {
        // Room for a discovery, its achievement chime and the next touches to ring together.
        const val MAX_STREAMS = 8
        const val EFFECT_VOLUME = 0.8f

        // The playback rates SoundPool accepts.
        const val MIN_RATE = 0.5f
        const val MAX_RATE = 2f
    }
}

/** The looping background track, created on first start and kept paused while the app is hidden. */
class BackgroundMusic(private val context: Context) {
    private var current: MediaPlayer? = null
    private var upcoming: MediaPlayer? = null
    private var fade: ValueAnimator? = null
    private var volume = 0f
    private var audible = false
    private val handler = Handler(Looper.getMainLooper())
    private val restore = Runnable { if (audible) fadeTo(MUSIC_VOLUME * level, RESTORE_MILLIS) {} }

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
        handler.removeCallbacks(restore)
        fadeTo(MUSIC_VOLUME * level, FADE_MILLIS) {}
    }

    fun pause() {
        audible = false
        handler.removeCallbacks(restore)
        val playing = current?.takeIf { it.isPlaying } ?: return
        fadeTo(0f, FADE_MILLIS) { playing.pause() }
    }

    /** Lowers the music under a big moment for [holdMillis], then lets it swell back. */
    fun duck(holdMillis: Long) {
        if (!audible) return
        handler.removeCallbacks(restore)
        fadeTo(MUSIC_VOLUME * level * DUCK_LEVEL, DUCK_MILLIS) {}
        handler.postDelayed(restore, holdMillis)
    }

    fun release() {
        handler.removeCallbacks(restore)
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

    private fun fadeTo(target: Float, millis: Long, onEnd: () -> Unit) {
        fade?.cancel()
        fade = ValueAnimator.ofFloat(volume, target).apply {
            duration = millis
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

        // Under a discovery the music steps back to a third, quickly, and returns slowly.
        const val DUCK_LEVEL = 0.35f
        const val DUCK_MILLIS = 120L
        const val RESTORE_MILLIS = 900L
    }
}

/** One primitive of a composed vibration: its strength from 0 to 1 and the pause before it. */
class HapticStep(val primitive: Int, val scale: Float, val delayMillis: Int = 0)

/**
 * A touch vibration. The bigger moments are composed from the platform's primitives, which feel crisp on phones
 * with a good actuator; elsewhere they fall back to the predefined [effectId].
 */
enum class Haptic(val effectId: Int, val steps: List<HapticStep> = emptyList()) {
    TICK(VibrationEffect.EFFECT_TICK),
    CLICK(VibrationEffect.EFFECT_CLICK),
    HEAVY(VibrationEffect.EFFECT_HEAVY_CLICK),
    DOUBLE(VibrationEffect.EFFECT_DOUBLE_CLICK),

    // A quick swell that lands on a crisp click: something new has formed.
    DISCOVER(
        VibrationEffect.EFFECT_DOUBLE_CLICK,
        listOf(HapticStep(Composition.PRIMITIVE_QUICK_RISE, 0.5f), HapticStep(Composition.PRIMITIVE_CLICK, 1f, 30))
    ),

    // A deep thud, a spin and a final click for an epic or legendary find.
    DISCOVER_GRAND(
        VibrationEffect.EFFECT_HEAVY_CLICK,
        listOf(
            HapticStep(Composition.PRIMITIVE_THUD, 1f),
            HapticStep(Composition.PRIMITIVE_SPIN, 0.6f, 80),
            HapticStep(Composition.PRIMITIVE_CLICK, 1f, 60)
        )
    ),

    // Three light taps, like a small fanfare.
    ACHIEVEMENT(
        VibrationEffect.EFFECT_CLICK,
        listOf(
            HapticStep(Composition.PRIMITIVE_TICK, 0.6f),
            HapticStep(Composition.PRIMITIVE_TICK, 0.8f, 80),
            HapticStep(Composition.PRIMITIVE_CLICK, 1f, 80)
        )
    )
}

/** Touch vibrations, which follow the system's touch feedback strength. */
class Haptics(context: Context) {
    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator

    fun perform(haptic: Haptic) {
        if (!vibrator.hasVibrator()) return
        val effect = composed(haptic) ?: VibrationEffect.createPredefined(haptic.effectId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            vibrator.vibrate(effect)
        }
    }

    private fun composed(haptic: Haptic): VibrationEffect? {
        val primitives = haptic.steps.map(HapticStep::primitive).distinct().toIntArray()
        if (primitives.isEmpty() || !vibrator.areAllPrimitivesSupported(*primitives)) return null
        return haptic.steps
            .fold(VibrationEffect.startComposition()) { composition, step -> composition.addPrimitive(step.primitive, step.scale, step.delayMillis) }
            .compose()
    }
}
