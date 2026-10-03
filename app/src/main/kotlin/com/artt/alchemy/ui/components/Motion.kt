package com.artt.alchemy.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** True when decorative animation is off: the system animation scale is zero. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** The same animation, or an instant jump to the end value when motion is reduced. */
@Composable
fun <T> motion(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> = if (LocalReducedMotion.current) snap() else spec

/** Whether the system has animations switched off (its animator duration scale is zero). */
fun systemAnimationsOff(context: Context): Boolean = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
