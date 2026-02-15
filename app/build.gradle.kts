plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.devendrap7.phonemoodtranslator"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.devendrap7.phonemoodtranslator"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "2.0"

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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.github.yukuku:ambilwarna:2.0.1")

    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor ("androidx.room:room-compiler:2.6.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("com.google.android.material:material:1.13.0")
    // JSON Converter (To save lists of apps)
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("androidx.work:work-runtime:2.9.0")
}