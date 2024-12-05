import io.sentry.android.gradle.instrumentation.logcat.LogcatLevel
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.kotlinAndroidKsp)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.detekt)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.crashlyticsGradle)

    id("io.sentry.android.gradle") version "4.14.1"
}

fun getVersionName(): String {
    val properties = Properties()
    properties.load(file("gradle.properties").inputStream())
    return properties.getProperty("VERSION_NAME") ?: "1.0"
}

fun getVersionCode() : Int {
    val properties = Properties()
    properties.load(file("gradle.properties").inputStream())
    return properties.getProperty("VERSION_CODE")?.toInt() ?: 1
}

android {
    namespace = "com.vismo.nextgenmeter"
    compileSdk = 34

    signingConfigs {
        create("myConfig") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = file("..\\keystore\\debug.keystore")
            storePassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.vismo.nextgenmeter"
        minSdk = 24
        targetSdk = 34
        versionCode = getVersionCode()
        versionName = getVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("myConfig")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("myConfig")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    detekt {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom("$projectDir/config/detekt/config.yml")
        baseline = file("$projectDir/config/detekt/baseline.xml")
    }

    flavorDimensions.add("env")
    productFlavors {
        create("dev") {
            dimension = "env"
//            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            archivesName = "cablemeter-$${getVersionName()}(${getVersionCode()})"
        }
        create("dev2") {
            dimension = "env"
//            applicationIdSuffix = ".dev2"
            versionNameSuffix = "-dev2"
            archivesName = "cablemeter-$${getVersionName()}(${getVersionCode()})"
        }
        create("qa") {
            dimension = "env"
//            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            archivesName = "cablemeter-${getVersionName()}(${getVersionCode()})"
        }
        create("prd") {
            dimension = "env"
            archivesName = "cablemeter-${getVersionName()}(${getVersionCode()})"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.process)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(project(":measure-board-module"))
    implementation(project(":NxGnFirebaseModule"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.detekt.gradle)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.auth.java.jwt)
    implementation(libs.amap.location)
    implementation(libs.compose.material.icons)
    implementation(libs.lightspark.compose.qr)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    implementation(libs.commons.net)
    implementation(libs.samstevens.totp)
    implementation(libs.preferences.datastore)
    implementation(libs.floating.bubble.view)
    implementation(libs.proto.datastore)
}


sentry {
    org.set("vis-mobility-limited")
    projectName.set("cable-meter")

    // this will upload your source code to Sentry to show it as part of the stack traces
    // disable if you don't want to expose your sources
    includeSourceContext.set(true)

    tracingInstrumentation {
        enabled.set(true)

        logcat {
            enabled.set(true)
            minLevel.set(LogcatLevel.VERBOSE) // change later when it is past the testing phase
        }
    }
}
