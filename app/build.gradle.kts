plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.nishant.smartattendance"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nishant.smartattendance"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA"
            )
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data"))

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // UI
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // PDF export
    implementation("com.itextpdf:itext7-core:7.2.5")

    // Excel export
    implementation("org.dhatim:fastexcel:0.15.7")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    // Guava — required for CameraX ListenableFuture
    implementation("com.google.guava:guava:32.1.3-android")

    // ML Kit Face Detection
    implementation("com.google.mlkit:face-detection:16.1.5")

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}