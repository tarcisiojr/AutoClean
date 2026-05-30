plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// versionName é fonte única de verdade — atualizado pelo release-please.
// versionCode é derivado automaticamente: MAJOR*10000 + MINOR*100 + PATCH.
val appVersionName = "2.0.0" // x-release-please-version
val appVersionCode = appVersionName.split(".").let { parts ->
    require(parts.size == 3) { "versionName precisa seguir SemVer (MAJOR.MINOR.PATCH): $appVersionName" }
    parts[0].toInt() * 10_000 + parts[1].toInt() * 100 + parts[2].toInt()
}

android {
    namespace = "com.autoclean"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.autoclean"
        minSdk = 31
        targetSdk = 31
        versionCode = appVersionCode
        versionName = appVersionName

        ndk {
            abiFilters += listOf("armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
