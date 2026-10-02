package com.artt.alchemy.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibrationEffect.Composition
import android.os.VibratorManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
import com.artt.alchemy.R
import kotlin.random.Random

/**
 * A sound effect; [pitchSpread] is how far each play may drift from the recorded pitch, so repeats never sound canned,
 * and [gain] evens out files mastered hotter than the rest, so the mix keeps its headroom.
 */
enum class Sound(@param:RawRes val res: Int, val pitchSpread: Float = 0f, val gain: Float = 1f) {
    PLACE(R.raw.sfx_place, 0.06f),
    COMBINE(R.raw.sfx_combine, 0.02f, gain = 0.5f),
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

private val EffectAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_GAME)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

private val MusicAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_GAME)
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .build()

/** A play asked for before its sound finished loading, kept to be played the moment it is ready. */
private class PendingPlay(val left: Float, val right: Float, val rate: Float, val askedAt: Long)

/**
 * Short effects, loaded up front so they play without delay. SoundPool loads in the background, so a sound asked for
 * in the first moments after launch waits for its load and plays then, unless it would come too late to belong.
 */
class SoundEffects(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(EffectAttributes).build()
    private val loaded = HashSet<Int>()
    private val pending = HashMap<Int, PendingPlay>()

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == LOAD_SUCCESS) loaded += sampleId
            val play = pending.remove(sampleId) ?: return@setOnLoadCompleteListener
            if (status == LOAD_SUCCESS && SystemClock.uptimeMillis() - play.askedAt <= LATE_MILLIS) {
                pool.play(sampleId, play.left, play.right, 1, 0, play.rate)
            }
        }
    }

    private val ids = Sound.entries.associateWith { pool.load(context, it.res, 1) }

    /** The player's effects volume from 0 to 1, applied to every effect started after it is set. */
    var level = 1f

    /**
     * Plays [sound] leaning towards [pan] (-1 left to 1 right) and shifted by [semitones], with a small random drift
     * in pitch for the sounds that repeat often.
     */
    fun play(sound: Sound, pan: Float = 0f, semitones: Float = 0f) {
        val volume = EFFECT_VOLUME * level * sound.gain
        val (left, right) = stereoGains(pan)
        val drift = if (sound.pitchSpread > 0f) Random.nextFloat() * 2f * sound.pitchSpread - sound.pitchSpread else 0f
        val rate = (semitoneRate(semitones) * (1f + drift)).coerceIn(MIN_RATE, MAX_RATE)
        val id = ids.getValue(sound)
        if (id in loaded) {
            pool.play(id, volume * left, volume * right, 1, 0, rate)
        } else {
            pending[id] = PendingPlay(volume * left, volume * right, rate, SystemClock.uptimeMillis())
        }
    }

    fun release() = pool.release()

    private companion object {
        // Room for a discovery, its achievement chime and the next touches to ring together.
        const val MAX_STREAMS = 8

        // Leaves headroom for several effects and the music sounding at once.
        const val EFFECT_VOLUME = 0.42f

        // The playback rates SoundPool accepts.
        const val MIN_RATE = 0.5f
        const val MAX_RATE = 2f

        const val LOAD_SUCCESS = 0

        // A sound still loading this long after it was asked for no longer matches what is on screen.
        const val LATE_MILLIS = 250L
    }
}

/**
 * The game's claim on audio output while its music plays. Other apps pause or duck for it; a call or another player
 * taking over reports back through [onChange].
 */
private class AudioFocus(context: Context, onChange: (Int) -> Unit) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(MusicAttributes)
        .setOnAudioFocusChangeListener({ change -> onChange(change) }, Handler(Looper.getMainLooper()))
        .build()

    fun request(): Boolean = manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    fun abandon() {
        manager.abandonAudioFocusRequest(request)
    }
}

/**
 * The looping background track, created on first start and kept paused while the app is hidden. It holds audio focus
 * only while it plays, so a player who turned the music off keeps listening to their own.
 */
class BackgroundMusic(private val context: Context) {
    private var current: MediaPlayer? = null
    private var upcoming: MediaPlayer? = null

    // Players are prepared off the main thread; one still preparing is kept so it can be released.
    private val preparing = HashSet<MediaPlayer>()
    private var released = false
    private var fade: ValueAnimator? = null
    private var volume = 0f
    private var audible = false
    private var noisyReceiverRegistered = false
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()
        }
    }

    // The game is on screen with music on; it may still be silent while another app holds the focus.
    private var wanted = false
    private var introPlayed = false
    private val handler = Handler(Looper.getMainLooper())
    private val restore = Runnable { if (audible) fadeTo(MUSIC_VOLUME * level, RESTORE_MILLIS) {} }
    private val intro = Runnable { play(INTRO_FADE_MILLIS) }
    private val focus = AudioFocus(context, ::onFocusChange)

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
        wanted = true
        if (!noisyReceiverRegistered) {
            ContextCompat.registerReceiver(context, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
            noisyReceiverRegistered = true
        }
        handler.removeCallbacks(intro)
        if (!focus.request()) return
        // At launch the scene settles for a moment in silence, then the music comes in slowly.
        if (introPlayed) play(FADE_IN_MILLIS) else handler.postDelayed(intro, INTRO_DELAY_MILLIS)
    }

    fun pause() {
        wanted = false
        unregisterNoisyReceiver()
        silence()
        focus.abandon()
    }

    private fun play(fadeMillis: Long) {
        introPlayed = true
        audible = true
        handler.removeCallbacks(restore)
        val playing = current
        if (playing == null) {
            startFirstPlayer()
        } else if (!playing.isPlaying) {
            playing.start()
        }
        // The fade runs on the volume alone, so a player still preparing joins it where it has got to.
        fadeTo(MUSIC_VOLUME * level, fadeMillis) {}
    }

    private fun startFirstPlayer() {
        if (preparing.isNotEmpty()) return
        newPlayer { first ->
            current = first
            chain(first)
            if (audible) first.start()
        }
    }

    private fun silence() {
        audible = false
        handler.removeCallbacks(restore)
        handler.removeCallbacks(intro)
        if (current?.isPlaying != true) return
        // The loop may replace and release the player during the fade. Pause its live successor.
        fadeTo(0f, FADE_OUT_MILLIS) { current?.takeIf { it.isPlaying }?.pause() }
    }

    // A call or another player takes over: give way, and come back when a passing interruption ends. After a
    // permanent loss the music waits for the game to come back on screen. Ducking is left to the system.
    private fun onFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> if (wanted && !audible) play(FADE_IN_MILLIS)

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> silence()

            AudioManager.AUDIOFOCUS_LOSS -> pause()
        }
    }

    /** Lowers the music under a big moment for [holdMillis], then lets it swell back. */
    fun duck(holdMillis: Long) {
        if (!audible) return
        handler.removeCallbacks(restore)
        fadeTo(MUSIC_VOLUME * level * DUCK_LEVEL, DUCK_MILLIS) {}
        handler.postDelayed(restore, holdMillis)
    }

    fun release() {
        wanted = false
        audible = false
        unregisterNoisyReceiver()
        handler.removeCallbacks(restore)
        handler.removeCallbacks(intro)
        focus.abandon()
        fade?.cancel()
        fade = null
        released = true
        current?.release()
        upcoming?.release()
        preparing.forEach(MediaPlayer::release)
        preparing.clear()
        current = null
        upcoming = null
    }

    private fun unregisterNoisyReceiver() {
        if (!noisyReceiverRegistered) return
        context.unregisterReceiver(noisyReceiver)
        noisyReceiverRegistered = false
    }

    // Looping a single MediaPlayer leaves an audible gap at the loop point. A second player, already
    // prepared, takes over the moment the first ends, so the track repeats without a break.
    private fun chain(player: MediaPlayer) {
        newPlayer { next ->
            // The track may have ended, or the music been released, while the next player was preparing.
            when {
                current === player -> {
                    upcoming = next
                    player.setNextMediaPlayer(next)
                }

                // The track ended before its successor was ready: the successor carries on, after a short gap.
                current == null -> {
                    current = next
                    chain(next)
                    if (audible) next.start()
                }

                else -> next.release()
            }
        }
        player.setOnCompletionListener { finished ->
            finished.release()
            current = upcoming
            upcoming = null
            val nextPlayer = current
            if (nextPlayer == null) {
                // The next player is still preparing and takes over when ready; if it failed, start afresh.
                if (audible) startFirstPlayer()
            } else {
                // setNextMediaPlayer starts this player automatically, even during a pending pause.
                if (!audible && nextPlayer.isPlaying) nextPlayer.pause()
                chain(nextPlayer)
            }
        }
    }

    /** Prepares a player of the track off the main thread and hands it over, at the current volume, once it is ready. */
    private fun newPlayer(onReady: (MediaPlayer) -> Unit) {
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(MusicAttributes)
            context.resources.openRawResourceFd(R.raw.music_background).use { player.setDataSource(it) }
        } catch (_: Exception) {
            player.release()
            return
        }
        preparing += player
        player.setOnPreparedListener { ready ->
            preparing -= ready
            if (released) {
                ready.release()
            } else {
                ready.setVolume(volume, volume)
                onReady(ready)
            }
        }
        player.setOnErrorListener { failed, _, _ ->
            preparing -= failed
            false
        }
        player.prepareAsync()
    }

    private fun fadeTo(target: Float, millis: Long, onEnd: () -> Unit) {
        fade?.cancel()
        fade = ValueAnimator.ofFloat(volume, target).apply {
            duration = millis
            // The ear hears loudness, not amplitude: a rise starts gently instead of jumping out of silence.
            interpolator = if (target > volume) AccelerateInterpolator(FADE_IN_CURVE) else DecelerateInterpolator()
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
        const val MUSIC_VOLUME = 0.13f
        const val FADE_IN_MILLIS = 900L
        const val FADE_OUT_MILLIS = 600L
        const val FADE_IN_CURVE = 1.6f
        const val INTRO_DELAY_MILLIS = 400L
        const val INTRO_FADE_MILLIS = 1600L

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
