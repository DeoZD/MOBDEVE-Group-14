plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mobdeve.s15.group14.fridyi"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mobdeve.s15.group14.fridyi"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ML Kit Text Recognition (Free, On-Device)
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Generative AI SDK (Optional for Gemini integration)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Coil for image loading
    implementation("io.coil-kt:coil:2.7.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}