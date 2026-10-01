package com.artt.alchemy.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.artt.alchemy.MainActivity
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain = skippingOnboarding(composeRule)

    @Test
    fun settings_toggles_and_confirmed_reset_update_visible_progress() {
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_reset").performScrollTo().performClick()
        composeRule.onNodeWithText("Сбросить прогресс?").assertIsDisplayed()
        composeRule.onNodeWithText("Сбросить").performClick()

        composeRule.onNodeWithTag("settings_sound").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_sound").performScrollTo().assertIsOff()

        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.onNodeWithText("4 / 180").assertIsDisplayed()

        // The four base elements come for free and do not count towards achievements.
        composeRule.onNodeWithTag("nav_achievements").performClick()
        composeRule.onNodeWithContentDescription("Первые открытия: 0 / 10").assertIsDisplayed()
    }

    @Test
    fun feedbackSettingsPersistAcrossActivityRecreation() {
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_reset").performScrollTo().performClick()
        composeRule.onNodeWithText("Сбросить").performClick()

        composeRule.onNodeWithTag("settings_sound").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_vibration").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_music").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_sound").performScrollTo().assertIsOff()
        composeRule.onNodeWithTag("settings_vibration").performScrollTo().assertIsOff()
        composeRule.onNodeWithTag("settings_music").performScrollTo().assertIsOff()

        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_sound").performScrollTo().assertIsOff()
        composeRule.onNodeWithTag("settings_vibration").performScrollTo().assertIsOff()
        composeRule.onNodeWithTag("settings_music").performScrollTo().assertIsOff()
    }

    @Test
    fun chosenThemeSurvivesActivityRecreation() {
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_theme_aether").performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag("settings_theme_ember").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_theme_ember").assertIsSelected()
        composeRule.onNodeWithTag("settings_theme_aether").assertIsNotSelected()

        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_theme_ember").performScrollTo().assertIsSelected()

        // Leave the default theme for the tests that follow.
        composeRule.onNodeWithTag("settings_theme_aether").performClick()
        composeRule.onNodeWithTag("settings_theme_aether").assertIsSelected()
    }
}
