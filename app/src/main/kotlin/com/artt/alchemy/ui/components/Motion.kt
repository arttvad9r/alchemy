package com.artt.alchemy.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** True when decorative animation is off: the player chose fewer animations, or the system animation scale is zero. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** The same animation, or an instant jump to the end value when motion is reduced. */
@Composable
fun <T> motion(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> = if (LocalReducedMotion.current) snap() else spec

/** What "reduced motion" means while the player has not chosen: the system's own animation scale. */
fun systemAnimationsOff(context: Context): Boolean = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
