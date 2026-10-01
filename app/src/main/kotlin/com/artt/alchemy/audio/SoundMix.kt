package com.artt.alchemy.audio

import kotlin.math.pow

// A sound at the very edge of the workspace leans this far to its side; the other side still hears it.
private const val MAX_PAN = 0.45f
private const val SEMITONES_PER_OCTAVE = 12f

/** Left and right gains for a sound heard at [pan], from -1 (far left) to 1 (far right); the centre plays at full volume on both sides. */
fun stereoGains(pan: Float): Pair<Float, Float> {
    val lean = pan.coerceIn(-1f, 1f) * MAX_PAN
    return (1f - lean).coerceAtMost(1f) to (1f + lean).coerceAtMost(1f)
}

/** The pan of a point on the workspace, from its left edge (0) to its right edge (1). */
fun panAt(xFraction: Float): Float = xFraction.coerceIn(0f, 1f) * 2f - 1f

/** The playback rate that shifts a sound by [semitones]. */
fun semitoneRate(semitones: Float): Float = 2f.pow(semitones / SEMITONES_PER_OCTAVE)

/**
 * Successful mixes made in quick succession: each one in a row plays a semitone higher, up to [maxSteps],
 * and a miss or a pause longer than [windowMillis] starts the run over.
 */
class ComboStreak(private val windowMillis: Long = DEFAULT_WINDOW_MILLIS, private val maxSteps: Int = DEFAULT_MAX_STEPS) {
    private var steps = -1
    private var lastMillis = 0L

    /** Counts a successful mix made at [nowMillis] and returns how many semitones it rises. */
    fun hit(nowMillis: Long): Int {
        steps = if (steps >= 0 && nowMillis - lastMillis <= windowMillis) (steps + 1).coerceAtMost(maxSteps) else 0
        lastMillis = nowMillis
        return steps
    }

    fun reset() {
        steps = -1
    }

    private companion object {
        const val DEFAULT_WINDOW_MILLIS = 4000L
        const val DEFAULT_MAX_STEPS = 5
    }
}
