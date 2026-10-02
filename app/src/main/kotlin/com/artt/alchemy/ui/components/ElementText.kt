package com.artt.alchemy.ui.components

import android.annotation.SuppressLint
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource

/** An element's name in the current language; texts live in `elements.xml` under `element_name_<id>`. */
fun Resources.elementName(id: String): String = getString(stringId("element_name_$id"))

/** An element's fact in the current language; texts live in `elements.xml` under `element_fact_<id>`. */
fun Resources.elementFact(id: String): String = getString(stringId("element_fact_$id"))

@Composable
fun elementName(id: String): String = stringResource(elementStringId("element_name_$id"))

@Composable
fun elementFact(id: String): String = stringResource(elementStringId("element_fact_$id"))

@Composable
private fun elementStringId(key: String): Int {
    val resources = LocalResources.current
    return remember(key, resources) { resources.stringId(key) }
}

// Texts are looked up by name so 180 elements need no hand-kept table of ids; res/raw/keep.xml keeps every
// element_name_* and element_fact_* through resource shrinking, and ElementTextsTest checks each one exists.
@SuppressLint("DiscouragedApi")
private fun Resources.stringId(key: String): Int = getIdentifier(key, "string", "com.artt.alchemy").also {
    check(it != 0) { "Missing string resource $key" }
}
