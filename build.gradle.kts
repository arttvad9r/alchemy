// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.compose.compiler) apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
    id("dev.detekt") version "2.0.0-alpha.6" apply false
}

// NormalPowers android-engineering-v1 bootstrap
tasks.register("qualityCheck") {
    group = "verification"
    dependsOn(":app:ktlintCheck", ":app:detekt", ":app:lintDebug", ":app:testDebugUnitTest")
}

tasks.register("formatCode") {
    group = "formatting"
    dependsOn(":app:ktlintFormat")
}
