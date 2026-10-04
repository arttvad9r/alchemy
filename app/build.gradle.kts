plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
}

android {
    namespace = "com.artt.alchemy"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.artt.alchemy"
        minSdk = 31
        targetSdk = 37
        versionCode = 5
        versionName = "1.2.1"
        testInstrumentationRunner = "com.artt.alchemy.RussianLocaleRunner"
    }

    // The release key stays out of the repository: its path and passwords come from
    // ~/.gradle/gradle.properties. Without them the release build is left unsigned.
    val releaseStoreFile = providers.gradleProperty("ALCHEMY_RELEASE_STORE_FILE").orNull
    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = providers.gradleProperty("ALCHEMY_RELEASE_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("ALCHEMY_RELEASE_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("ALCHEMY_RELEASE_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        aidl = false
        buildConfig = false
        shaders = false
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    // The Android stub of org.json throws in local unit tests.
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Compose UI tests bring Espresso 3.5, which cannot inject input on Android 17 (API 37).
    androidTestImplementation(libs.androidx.test.espresso.core)
}

// NormalPowers android-engineering-v1 bootstrap
detekt {
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = false
}

android {
    lint {
        abortOnError = true
        error += "HardcodedText"
    }
}
