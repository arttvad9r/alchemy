package com.artt.alchemy

import android.app.LocaleManager
import android.os.Bundle
import android.os.LocaleList
import androidx.test.runner.AndroidJUnitRunner

/** The UI tests look for Russian texts, so the app under test always runs in Russian whatever the device language is. */
class RussianLocaleRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        targetContext.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("ru")
        super.onCreate(arguments)
    }
}
