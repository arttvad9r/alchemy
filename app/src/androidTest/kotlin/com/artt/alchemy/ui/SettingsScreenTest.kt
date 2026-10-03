package com.artt.alchemy.ui

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.MainActivity
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class SettingsScreenTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    // Russian comes back once the activity is closed: switching back inside the test would recreate it a second time,
    // and the test rule can lose track of an activity recreated twice in a row.
    @get:Rule
    val chain: TestRule = RuleChain.outerRule(object : TestWatcher() {
        override fun finished(description: Description) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("ru")
            }
        }
    }).around(skippingOnboarding(composeRule))

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
    fun transferAndHelpOpenOnTheirOwnPagesAndBackLeadsToTheMainPage() {
        composeRule.onNodeWithTag("nav_settings").performClick()

        composeRule.onNodeWithTag("settings_help").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_page_help").assertIsDisplayed()
        composeRule.onNodeWithText("Как играть").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_back").performClick()
        composeRule.onNodeWithTag("screen_settings").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_transfer").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_export").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_import").assertIsDisplayed()
        // The system back goes to the main page first, not out of the game.
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("screen_settings").assertIsDisplayed()
    }

    @Test
    fun choosingALanguageSwitchesTheGameAndKeepsTheLanguagePageOpen() {
        // The game's own language choice exists from Android 13.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_language").performScrollTo().performClick()
        // The test runner sets Russian as the app's language.
        composeRule.onNodeWithTag("language_ru").assertIsSelected()
        composeRule.onNodeWithTag("language_en").assertIsNotSelected()
        composeRule.onNodeWithTag("language_system").assertIsNotSelected()
        composeRule.onNodeWithTag("language_en").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("Language").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("language_en").assertIsSelected()
        // The activity is recreated in English and stays on the language page.
        composeRule.onNodeWithTag("settings_page_language").assertIsDisplayed()
    }
}
