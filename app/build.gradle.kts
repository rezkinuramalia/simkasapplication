plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.simkasapp"
    compileSdk = 35 // <--- DIPERBARUI KE 35 (Solusi Error)

    defaultConfig {
        applicationId = "com.example.simkasapp"
        minSdk = 24
        targetSdk = 35 // <--- DIPERBARUI KE 35
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
        compose = true
    }
}

dependencies {
    // --- CORE ANDROID & COMPOSE ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0") // Update minor
    implementation("androidx.activity:activity-compose:1.9.0") // Update minor

    // BOM (Bill of Materials) - Mengatur versi Compose secara otomatis
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // Library UI Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // --- LIBRARY TAMBAHAN UNTUK PROJECT INI ---

    // 1. NAVIGASI (Pindah Layar)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 2. RETROFIT (Koneksi API)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

    // 3. COIL (Gambar)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- TESTING & DEBUGGING ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Tools Debugging
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}