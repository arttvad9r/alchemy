package com.artt.alchemy.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artt.alchemy.MainActivity
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settings_toggles_and_confirmed_reset_update_visible_progress() {
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_reset").performClick()
        composeRule.onNodeWithText("Сбросить прогресс?").assertIsDisplayed()
        composeRule.onNodeWithText("Сбросить").performClick()

        composeRule.onNodeWithTag("settings_sound").performClick()
        composeRule.onNodeWithTag("settings_sound").assertIsOff()

        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.onNodeWithText("4 / 180").assertIsDisplayed()

        // The four base elements come for free and do not count towards achievements.
        composeRule.onNodeWithTag("nav_achievements").performClick()
        composeRule.onNodeWithText("Первые открытия: 0 / 10").assertIsDisplayed()
    }

    @Test
    fun feedbackSettingsPersistAcrossActivityRecreation() {
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_reset").performClick()
        composeRule.onNodeWithText("Сбросить").performClick()

        composeRule.onNodeWithTag("settings_sound").performClick()
        composeRule.onNodeWithTag("settings_vibration").performClick()
        composeRule.onNodeWithTag("settings_music").performClick()
        composeRule.onNodeWithTag("settings_sound").assertIsOff()
        composeRule.onNodeWithTag("settings_vibration").assertIsOff()
        composeRule.onNodeWithTag("settings_music").assertIsOff()

        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_sound").assertIsOff()
        composeRule.onNodeWithTag("settings_vibration").assertIsOff()
        composeRule.onNodeWithTag("settings_music").assertIsOff()
    }
}
