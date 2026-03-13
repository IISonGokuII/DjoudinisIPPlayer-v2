plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.djoudini.iplayer"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.djoudini.iplayer"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            // Uses default debug keystore
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            buildConfigField("String", "DEFAULT_USER_AGENT", "\"VLC/3.0.20 LibVLC/3.0.20\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "DEFAULT_USER_AGENT", "\"VLC/3.0.20 LibVLC/3.0.20\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // Compose for TV
    implementation(libs.tv.compose.foundation)
    implementation(libs.tv.compose.material)

    // Activity & Core
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Media3 (ExoPlayer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.media3.common)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Retrofit converter for Kotlinx Serialization
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // WorkManager
    implementation(libs.work.runtime)

    // Coil
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Timber
    implementation(libs.timber)

    // Firebase Crashlytics (Online Crash Reporting)
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.7.6")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.6")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}
