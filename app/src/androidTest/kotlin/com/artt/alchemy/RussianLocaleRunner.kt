package com.artt.alchemy

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.test.runner.AndroidJUnitRunner
import java.util.Locale

/** The UI tests look for Russian texts, so the app under test always runs in Russian whatever the device language is. */
class RussianLocaleRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        Locale.setDefault(Locale.forLanguageTag("ru"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            targetContext.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("ru")
        }
        super.onCreate(arguments)
    }

    override fun getTargetContext(): Context {
        val context = super.getTargetContext()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context
        } else {
            context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocales(LocaleList.forLanguageTags("ru")) })
        }
    }

    override fun newActivity(cl: ClassLoader, className: String, intent: Intent): Activity = super.newActivity(cl, className, intent).also { activity ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            activity.applyOverrideConfiguration(Configuration().apply { setLocales(LocaleList.forLanguageTags("ru")) })
        }
    }
}
